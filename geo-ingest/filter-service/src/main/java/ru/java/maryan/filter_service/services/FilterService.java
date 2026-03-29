package ru.java.maryan.filter_service.services;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.java.maryan.geo_common.dto.geo_ingest.BaseStationMessage;
import ru.java.maryan.geo_common.services.MessageHandler;
import ru.java.maryan.geo_common.services.MessageSender;
import ru.java.maryan.filter_service.utils.FilterUtil;

import static ru.java.maryan.geo_common.constants.KafkaConstants.TRACE_ID;

@Slf4j
@Component
public class FilterService implements MessageHandler<BaseStationMessage> {

    private final MessageSender<BaseStationMessage> sender;

    @Value("${spring.kafka.consumer.topic-out}")
    private String outputTopic;

    @Autowired
    public FilterService(MessageSender<BaseStationMessage> sender) {
        this.sender = sender;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.consumer.topic-in}")
    public void handle(BaseStationMessage message) {
        try (var ignored = MDC.putCloseable(TRACE_ID, message.getTraceId())) {
            log.debug("Received raw message: {}", message.eventType());

            if (FilterUtil.filter(message)) {
                log.info("Message passed validation. Forwarding to topic: {}", outputTopic);
                sender.send(message, outputTopic);
            } else {
                log.warn("Message dropped due to validation failure.");
            }
        }
    }
}
