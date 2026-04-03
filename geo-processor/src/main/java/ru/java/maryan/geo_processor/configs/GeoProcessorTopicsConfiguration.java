package ru.java.maryan.geo_processor.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class GeoProcessorTopicsConfiguration {
    @Value("${spring.kafka.consumer.topic-out}")
    private String topicName;

    @Value("${spring.kafka.consumer.topic.partitions}")
    private Integer partitions;

    @Value("${spring.kafka.consumer.topic.replicas}")
    private Integer replicas;

    @Bean
    public NewTopic triggerTopic() {
        return TopicBuilder
                .name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
