package ru.java.maryan.deduplication_service.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class DeduplicationTopicsConfiguration {

    @Value("${spring.kafka.consumer.topic-out}")
    private String topicName;

    @Value("${spring.kafka.consumer.topic.partitions}")
    private Integer partitions;

    @Value("${spring.kafka.consumer.topic.replicas}")
    private Integer replicas;

    @Bean
    public NewTopic deduplicatedStationsTopic() {
        return TopicBuilder
                .name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
