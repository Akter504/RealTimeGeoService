package ru.java.maryan.deduplication_service.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.java.maryan.deduplication_service.metrics.DeduplicationServiceMetrics;
import ru.java.maryan.deduplication_service.services.DeduplicationRedisStorage;
import ru.java.maryan.geo_common.dto.geo_ingest.BaseStationMessage;
import ru.java.maryan.geo_common.services.MessageHandler;
import ru.java.maryan.geo_common.services.MessageSender;

import static ru.java.maryan.geo_common.constants.KafkaConstants.TRACE_ID;

@Slf4j
@Component
public class DeduplicationService implements MessageHandler<BaseStationMessage> {

    private final MessageSender<BaseStationMessage> sender;
    private final DeduplicationServiceMetrics serviceMetrics;
    private final DeduplicationRedisStorage<BaseStationMessage> storage;

    @Value("${spring.kafka.consumer.topic-out}")
    private String outputTopic;

    @Autowired
    public DeduplicationService(MessageSender<BaseStationMessage> sender,
                                DeduplicationServiceMetrics serviceMetrics,
                                DeduplicationRedisStorage<BaseStationMessage> storage) {
        this.sender = sender;
        this.serviceMetrics = serviceMetrics;
        this.storage = storage;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.consumer.topic-in}")
    public void handle(BaseStationMessage message) {
        try (var ignored = MDC.putCloseable(TRACE_ID, message.getTraceId())) {
            log.debug("Checking message for duplicates...");
            serviceMetrics.recordReceived();
            if (storage.save(message)) {
                log.info("Message is unique. Forwarding to topic: {}", outputTopic);
                serviceMetrics.recordUnique();
                sender.send(message, outputTopic);
            } else {
                log.warn("Duplicate message detected. Dropping.");
                serviceMetrics.recordDuplicate();
            }
        }
    }
}
