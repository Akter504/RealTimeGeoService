package ru.java.maryan.geo_processor.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.java.maryan.geo_common.dto.geo_ingest.EnrichedBaseStationMessage;
import ru.java.maryan.geo_common.services.MessageHandler;
import ru.java.maryan.geo_common.services.MessageSender;
import ru.java.maryan.geo_processor.dto.LocationTriggerEvent;
import ru.java.maryan.geo_processor.metrics.GeoProcessorMetrics;
import ru.java.maryan.geo_processor.records.LastKnownLocationRecord;

import java.time.Duration;
import java.time.Instant;

import static ru.java.maryan.geo_common.constants.KafkaConstants.TRACE_ID;
import static ru.java.maryan.geo_common.constants.StationMessage.COLON;
import static ru.java.maryan.geo_common.constants.StationMessage.IMSI;
import static ru.java.maryan.geo_processor.constants.GeoProcessorConstants.*;

@Slf4j
@Service
public class GeoProcessorTriggerService implements MessageHandler<EnrichedBaseStationMessage> {
    private final MessageSender<LocationTriggerEvent> sender;
    private final GeoProcessorMetrics processorMetrics;
    private final ObjectMapper mapper;
    private final StringRedisTemplate redisTemplate;
    private final HomeWorkStatusService homeWorkStatusService;

    @Value("${spring.kafka.consumer.topic-out}")
    private String outputTopic;

    @Value("${geo.processor.lkl-ttl:1d}")
    private Duration lklTtl;

    @Autowired
    public GeoProcessorTriggerService(MessageSender<LocationTriggerEvent> sender,
                                      GeoProcessorMetrics processorMetrics,
                                      ObjectMapper mapper,
                                      StringRedisTemplate redisTemplate,
                                      HomeWorkStatusService homeWorkStatusService) {
        this.sender = sender;
        this.processorMetrics = processorMetrics;
        this.mapper = mapper;
        this.redisTemplate = redisTemplate;
        this.homeWorkStatusService = homeWorkStatusService;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.consumer.topic-in}", groupId = "${spring.kafka.consumer.trigger-group-id}")
    public void handle(EnrichedBaseStationMessage message) {
        processorMetrics.recordReceived();
        String imsi = message.imsi();
        if (imsi == null) {
            log.error("Imsi is null in geo processor.");
            return;
        }

        try (var ignored = MDC.putCloseable(TRACE_ID, IMSI + COLON + message.imsi())) {
            log.debug("Received enriched message for IMSI: {}. Starting trigger processing...", imsi);
            processMovementTriggers(imsi, message);
        }

    }

    private void processMovementTriggers(String imsi, EnrichedBaseStationMessage msg) {
        String redisKey = LAST_KNOWN_LOCATION_PREFIX + COLON + imsi;
        String newLac = msg.lac();
        String cellId = msg.rawCellId().toString();
        Instant timestamp = msg.timestamp();
        
        try {
            String jsonValue = redisTemplate.opsForValue().get(redisKey);
            int currentStatus = homeWorkStatusService.resolveStatus(imsi, newLac, cellId);
            if (jsonValue == null) {
                log.info("New subscriber detected: {}. Triggering ENTRY to LAC: {}", imsi, newLac);
                sendTrigger(imsi, TRIGGER_TYPE_ENTRY, newLac, cellId, timestamp);
                updateData(redisKey, newLac, cellId, timestamp, msg.latitude(), msg.longitude(), imsi, currentStatus);
                processorMetrics.recordEntryTrigger();
            } else {
                LastKnownLocationRecord lkl = mapper.readValue(jsonValue, LastKnownLocationRecord.class);
                String oldLac = lkl.lac();
                String oldCellId = lkl.cellId();

                if (!oldLac.equals(newLac)) {
                    log.info("Subscriber {} moved from LAC {} to LAC {}", imsi, oldLac, newLac);
                    sendTrigger(imsi, TRIGGER_TYPE_EXIT, oldLac, oldCellId, msg.timestamp());
                    processorMetrics.recordExitTrigger();

                    sendTrigger(imsi, TRIGGER_TYPE_ENTRY, newLac, cellId, timestamp);
                    updateData(redisKey, newLac, cellId, timestamp, msg.latitude(), msg.longitude(), imsi, currentStatus);
                    processorMetrics.recordEntryTrigger();
                } else {
                    log.debug("Subscriber {} remains in LAC {}. No triggers fired.", imsi, newLac);
                    updateData(redisKey, newLac, cellId, timestamp, msg.latitude(), msg.longitude(), imsi, currentStatus);
                }
            }
        } catch (Exception e) {
            log.error("Error processing triggers for subscriber {}", imsi, e);
        }
    }

    private void updateData(String redisKey,
                            String lac,
                            String cellId,
                            Instant timestamp,
                            Double lat,
                            Double lon,
                            String imsi,
                            Integer status) {
        LastKnownLocationRecord lkl = new LastKnownLocationRecord(cellId, lac, timestamp, lat, lon, status);
        try {
            redisTemplate.opsForValue().set(redisKey, mapper.writeValueAsString(lkl), lklTtl);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        redisTemplate.opsForZSet().add(
                ACTIVE_SUBSCRIBERS,
                imsi,
                timestamp.toEpochMilli()
        );

        if (lat != null && lon != null) {
            redisTemplate.opsForGeo().add(GEO_GLOBAL_MAP_KEY, new Point(lon, lat), imsi);
        } else {
            log.warn("Skipping geo add for IMSI: {} - lat or lon is null (lat={}, lon={})", imsi, lat, lon);
        }
    }

    private void sendTrigger(String subscriberId,
                             String type,
                             String lac,
                             String cellId,
                             Instant ts) {
        LocationTriggerEvent event = new LocationTriggerEvent(subscriberId, type, lac, cellId, ts);
        sender.send(event, outputTopic);
    }
}
