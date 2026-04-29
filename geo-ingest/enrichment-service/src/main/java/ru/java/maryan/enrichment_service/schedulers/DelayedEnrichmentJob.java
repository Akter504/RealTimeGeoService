package ru.java.maryan.enrichment_service.schedulers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.java.maryan.enrichment_service.metrics.EnrichmentServiceMetrics;
import ru.java.maryan.enrichment_service.services.EnrichmentService;
import ru.java.maryan.geo_common.dto.geo_ingest.BaseStationMessage;

import java.time.Duration;
import java.time.Instant;

import static ru.java.maryan.enrichment_service.constants.EnrichmentServiceConstants.BATCH_DELAYED_ENRICHMENT;
import static ru.java.maryan.geo_common.constants.KafkaConstants.TRACE_ID;
import static ru.java.maryan.geo_common.constants.KafkaConstants.UNRESOLVED_QUEUE;

@Slf4j
@Service
public class DelayedEnrichmentJob {

    private final StringRedisTemplate redisTemplate;
    private final EnrichmentServiceMetrics metrics;
    private final ObjectMapper objectMapper;
    private final EnrichmentService enrichmentService;

    @Autowired
    public DelayedEnrichmentJob(StringRedisTemplate redisTemplate,
                                EnrichmentServiceMetrics metrics,
                                ObjectMapper objectMapper,
                                EnrichmentService enrichmentService) {
        this.redisTemplate = redisTemplate;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.enrichmentService = enrichmentService;
    }

    @Scheduled(fixedDelay = 300000)
    public void processDelayedMessages() {
        log.info("Starting delayed enrichment job...");

        Long size = redisTemplate.opsForList().size(UNRESOLVED_QUEUE);
        if (size == null || size == 0) {
            return;
        }

        log.info("Found {} messages in delayed queue. Processing...", size);
        try (var ignored = MDC.putCloseable(TRACE_ID, BATCH_DELAYED_ENRICHMENT)) {
            for (int i = 0; i < size; i++) {
                String json = redisTemplate.opsForList().leftPop(UNRESOLVED_QUEUE);
                if (json == null) continue;

                try {
                    BaseStationMessage msg = objectMapper.readValue(json, BaseStationMessage.class);

                    if (isExpired(msg.timestamp())) {
                        log.debug("Message for IMSI {} expired (older than 1 hour). Dropping forever.", msg.imsi());
                        metrics.recordDropped();
                    } else {
                        enrichmentService.handle(msg);
                    }


                } catch (Exception e) {
                    log.error("Error processing delayed message", e);
                }
            }
        }
    }

    private boolean isExpired(Instant messageTimestamp) {
        if (messageTimestamp == null) return true;
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        return messageTimestamp.isBefore(oneHourAgo);
    }
}
