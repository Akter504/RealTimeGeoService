package ru.java.maryan.enrichment_service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.java.maryan.enrichment_service.metrics.EnrichmentServiceMetrics;
import ru.java.maryan.enrichment_service.records.TacCsvRecord;
import ru.java.maryan.enrichment_service.records.TowerCsvRecord;
import ru.java.maryan.geo_common.dto.geo_ingest.BaseStationMessage;
import ru.java.maryan.geo_common.dto.geo_ingest.EnrichedBaseStationMessage;
import ru.java.maryan.geo_common.services.MessageHandler;
import ru.java.maryan.geo_common.services.MessageSender;

import static ru.java.maryan.geo_common.constants.KafkaConstants.*;
import static ru.java.maryan.geo_common.constants.StationMessage.*;

@Slf4j
@Service
public class EnrichmentService implements MessageHandler<BaseStationMessage> {

    private final MessageSender<EnrichedBaseStationMessage> sender;
    private final EnrichmentServiceMetrics serviceMetrics;
    private final CellTowerService cellTowerService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.consumer.topic-out}")
    private String outputTopic;

    @Autowired
    public EnrichmentService(MessageSender<EnrichedBaseStationMessage> sender,
                             EnrichmentServiceMetrics serviceMetrics,
                             CellTowerService cellTowerService,
                             StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper) {
        this.sender = sender;
        this.serviceMetrics = serviceMetrics;
        this.cellTowerService = cellTowerService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.consumer.topic-in}")
    public void handle(BaseStationMessage message) {
        try (var ignored = MDC.putCloseable(TRACE_ID, message.getTraceId())) {
            log.debug("Starting enrichment process...");
            serviceMetrics.recordReceived();

            EnrichedBaseStationMessage enrichedMessage = enrich(message);
            if (enrichedMessage != null) {
                log.info("Successfully enriched message with Device [{}] and Location [Lat: {}, Lon: {}]",
                        enrichedMessage.deviceModel(), enrichedMessage.latitude(), enrichedMessage.longitude());
                serviceMetrics.recordEnriched();
                sender.send(enrichedMessage, outputTopic);
            } else {
                log.warn("Failed to enrich message. The message has been sent to the deferred queue.");
            }
        }
    }

    public EnrichedBaseStationMessage enrich(BaseStationMessage msg) {
        String imsi = resolveImsi(msg);
        String msisdn = resolveMsisdn(msg);
        if (imsi == null || msisdn == null) {
            postponeMessage(msg);
            return null;
        }

        String key = createKey(imsi, msg.lac(), msg.cellId());
        TowerCsvRecord towerCsvRecord = cellTowerService.findTower(key);
        if (towerCsvRecord == null) {
            log.warn("Tower not found in dictionary for key: {}", key);
            postponeMessage(msg);
            return null;
        }

        String tac = extractTacFromImei(msg.imei());
        TacCsvRecord tacCsvRecord = cellTowerService.findTac(tac);
        if (tacCsvRecord == null) {
            log.warn("Device TAC not found in dictionary: {}", tac);
            postponeMessage(msg);
            return null;
        }
        return createEnrichedMessage(msg, towerCsvRecord, tacCsvRecord, imsi, msisdn);
    }

    private void postponeMessage(BaseStationMessage msg) {
        try {
            String json = objectMapper.writeValueAsString(msg);
            redisTemplate.opsForList().rightPush(UNRESOLVED_QUEUE, json);
            serviceMetrics.recordPostponed();
        } catch (Exception e) {
            log.error("Failed to postpone message", e);
        }
    }

    private String resolveImsi(BaseStationMessage msg) {
        if (msg.imsi() != null && !msg.imsi().isBlank()) {
            return msg.imsi();
        }

        if (msg.msisdn() != null && !msg.msisdn().isBlank()) {
            String imsiFromDict = redisTemplate.opsForValue().get(MSISDN + COLON + msg.msisdn());
            if (imsiFromDict != null) {
                log.debug("Restored IMSI {} from MSISDN {}", imsiFromDict, msg.msisdn());
            }
            return imsiFromDict;
        }
        log.warn("Cannot resolve IMSI for MSISDN: {}. Sending to delayed queue.", msg.msisdn());
        return null;
    }

    private String resolveMsisdn(BaseStationMessage msg) {
        if (msg.msisdn() != null && !msg.msisdn().isBlank()) {
            return msg.msisdn();
        }

        if (msg.imsi() != null && !msg.imsi().isBlank()) {
            String msisdnFromDict = redisTemplate.opsForValue().get(IMSI + COLON + msg.imsi());
            if (msisdnFromDict != null) {
                log.debug("Restored MSISDN {} from IMSI {}", msisdnFromDict, msg.imsi());
            }
            return msisdnFromDict;
        }
        log.warn("Cannot resolve MSISDN for Imsi: {}. Sending to delayed queue.", msg.imsi());
        return null;
    }


    private EnrichedBaseStationMessage createEnrichedMessage(BaseStationMessage msg,
                                                             TowerCsvRecord tower,
                                                             TacCsvRecord device,
                                                             String imsi,
                                                             String msisdn) {
        return new EnrichedBaseStationMessage(
                msisdn,
                imsi,
                msg.imei(),
                extractMccFromImsi(imsi),
                extractMncFromImsi(imsi),
                msg.lac(),
                msg.rat(),
                Long.parseLong(msg.cellId()),
//              cellIdInfo.towerId(),
//              cellIdInfo.sectorId(),
                tower.getLat(),
                tower.getLon(),
//              tower.azimuth(),
//              calculateH3Index(tower.getLat(), tower.getLon()),
//              tower.regionCode(),
                device.getCompany(),
                device.getName(),
                msg.timestamp(),
                msg.durationSec(),
                msg.signalStrength()
        );
    }

    private String createKey(String imsi, String lac, String cellId) {
        return String.format("%s-%s-%s-%s",
                extractMccFromImsi(imsi),
                extractMncFromImsi(imsi),
                lac,
                cellId
        );
    }

    private String extractTacFromImei(String imei) {
        return TAC + COLON + imei.substring(0, 8);
    }

    private String extractMccFromImsi(String imsi) {
        return imsi.substring(0, 3);
    }

    private String extractMncFromImsi(String imsi) {
        String mnc = imsi.substring(3, 5);
        if (mnc.startsWith("0")) {
            return mnc.substring(1);
        }
        return mnc;
    }
}
