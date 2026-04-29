package ru.java.maryan.geo_processor.schedulers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.java.maryan.geo_common.services.MessageSender;
import ru.java.maryan.geo_processor.dto.LocationTriggerEvent;
import ru.java.maryan.geo_processor.metrics.GeoProcessorMetrics;
import ru.java.maryan.geo_processor.records.LastKnownLocationRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static ru.java.maryan.geo_common.constants.KafkaConstants.TRACE_ID;
import static ru.java.maryan.geo_common.constants.StationMessage.COLON;
import static ru.java.maryan.geo_processor.constants.GeoProcessorConstants.*;

@Slf4j
@Service
public class LocationEvictionJob {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper;
    private final GeoProcessorMetrics processorMetrics;
    private final MessageSender<LocationTriggerEvent> sender;

    @Value("${spring.kafka.consumer.topic-out}")
    private String outputTopic;

    @Value("${geo.processor.eviction.idle-timeout:1h}")
    private Duration idleTimeout;

    @Autowired
    public LocationEvictionJob(StringRedisTemplate redisTemplate,
                               ObjectMapper mapper,
                               GeoProcessorMetrics processorMetrics,
                               MessageSender<LocationTriggerEvent> sender) {
        this.redisTemplate = redisTemplate;
        this.mapper = mapper;
        this.processorMetrics = processorMetrics;
        this.sender = sender;
    }

    @Scheduled(fixedDelayString = "${geo.processor.eviction.run-interval:5m}")
    public void evictIdleSubscribers() {
        try (var ignored = MDC.putCloseable(TRACE_ID, EVICTION_JOB)) {
            log.info("Starting eviction job for idle subscribers...");
            long cutoffTime = Instant.now().toEpochMilli() - idleTimeout.toMillis();

            Set<String> idleSubscribers = redisTemplate.opsForZSet().rangeByScore(ACTIVE_SUBSCRIBERS, 0, cutoffTime);

            if (idleSubscribers == null || idleSubscribers.isEmpty()) {
                log.info("No idle subscribers found.");
                return;
            }

            log.info("Found {} idle subscribers. Generating EXIT triggers...", idleSubscribers.size());
            for (String imsi : idleSubscribers) {
                evictSubscriber(imsi);
            }
        }
    }

    private void evictSubscriber(String imsi) {
        String lklKey = LAST_KNOWN_LOCATION_PREFIX + COLON + imsi;

        try {
            String json = redisTemplate.opsForValue().get(lklKey);
            if (json != null) {
                LastKnownLocationRecord lkl = mapper.readValue(json, LastKnownLocationRecord.class);

                LocationTriggerEvent event = new LocationTriggerEvent(
                        imsi, TRIGGER_TYPE_EXIT, lkl.lac(), lkl.cellId(), Instant.now()
                );
                sender.send(event, outputTopic);
                log.info("Generated final EXIT trigger for idle subscriber: {}", imsi);
                processorMetrics.recordExitTrigger();

                redisTemplate.delete(lklKey);
            } else {
                log.error("CRITICAL DATA INCONSISTENCY: Subscriber {} found in active ZSET, but LKL record is missing! Cleaning up orphan record.", imsi);
            }

            redisTemplate.opsForZSet().remove(ACTIVE_SUBSCRIBERS, imsi);
            redisTemplate.opsForGeo().remove(GEO_GLOBAL_MAP_KEY, imsi);

        } catch (Exception e) {
            log.error("Failed to evict subscriber {}", imsi, e);
        }
    }
}