package ru.java.maryan.geo_common.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.java.maryan.geo_common.services.MessageSender;

@Slf4j
@Component
@Primary
public class KafkaMessageSender<T> implements MessageSender<T> {

    private final KafkaTemplate<String, T> kafkaTemplate;
    private final String defaultTopic;

    public KafkaMessageSender(KafkaTemplate<String, T> kafkaTemplate,
                              @Value("${spring.kafka.default-topic}") String defaultTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.defaultTopic = defaultTopic;
    }

    @Override
    public void send(T message) {
        send(message, defaultTopic);
    }

    @Override
    public void send(T message, String topic) {
        kafkaTemplate.send(topic, message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Sent to {}: {}, offset: {}",
                                topic, message, result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send to {}: {}", topic, message, ex);
                    }
                });
    }
}
