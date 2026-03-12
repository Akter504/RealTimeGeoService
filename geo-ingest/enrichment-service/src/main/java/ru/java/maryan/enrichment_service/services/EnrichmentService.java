package ru.java.maryan.enrichment_service.services;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.java.maryan.enrichment_service.records.TacCsvRecord;
import ru.java.maryan.enrichment_service.records.TowerCsvRecord;
import ru.java.maryan.geo_common.dto.geo_ingest.BaseStationMessage;
import ru.java.maryan.geo_common.dto.geo_ingest.EnrichedBaseStationMessage;
import ru.java.maryan.geo_common.services.MessageHandler;
import ru.java.maryan.geo_common.services.MessageSender;

@Slf4j
@Service
public class EnrichmentService implements MessageHandler<BaseStationMessage> {

    private final MessageSender<EnrichedBaseStationMessage> sender;
    private final CellTowerService cellTowerService;

    @Value("${spring.kafka.consumer.topic-out}")
    private String outputTopic;

    @Autowired
    public EnrichmentService(MessageSender<EnrichedBaseStationMessage> sender, CellTowerService cellTowerService) {
        this.sender = sender;
        this.cellTowerService = cellTowerService;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.consumer.topic-in}")
    public void handle(BaseStationMessage message) {
        String imsi = message.imsi() != null ? message.imsi() : "UNKNOWN";

        try (var ignored = MDC.putCloseable("imsi", imsi)) {
            log.debug("Starting enrichment process...");

            EnrichedBaseStationMessage enrichedMessage = enrich(message);
            if (enrichedMessage != null) {
                log.info("Successfully enriched message with Device [{}] and Location [Lat: {}, Lon: {}]",
                        enrichedMessage.deviceModel(), enrichedMessage.latitude(), enrichedMessage.longitude());
                sender.send(enrichedMessage, outputTopic);
            } else {
                log.warn("Failed to enrich message. Dropping.");
            }
        }
    }

    public EnrichedBaseStationMessage enrich(BaseStationMessage msg) {
        String key = createKey(msg);
        TowerCsvRecord towerCsvRecord = cellTowerService.findTower(key);
        if (towerCsvRecord == null) {
            log.warn("Tower not found in dictionary for key: {}", key);
            return null;
        }

        String tac = extractTacFromImei(msg.imei());
        TacCsvRecord tacCsvRecord = cellTowerService.findTac(tac);
        if (tacCsvRecord == null) {
            log.warn("Device TAC not found in dictionary: {}", tac);
            return null;
        }
        return createEnrichedMessage(msg, towerCsvRecord, tacCsvRecord);
    }

    private EnrichedBaseStationMessage createEnrichedMessage(BaseStationMessage msg,
                                                             TowerCsvRecord tower,
                                                             TacCsvRecord device) {
        return new EnrichedBaseStationMessage(
                msg.msisdn(),
                msg.imsi(),
                msg.imei(),
                extractMccFromImsi(msg.imsi()),
                extractMncFromImsi(msg.imsi()),
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

    private String createKey(BaseStationMessage msg) {
        String imsi = msg.imsi();
        return String.format("%s-%s-%s-%s",
                extractMccFromImsi(imsi),
                extractMncFromImsi(imsi),
                msg.lac(),
                msg.cellId()
        );
    }

    private String extractTacFromImei(String imei) {
        return imei.substring(0, 8);
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
