package ru.java.maryan.enrichment_service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import ru.java.maryan.geo_common.dto.geo_ingest.BaseStationMessage;
import ru.java.maryan.geo_common.dto.geo_ingest.EnrichedBaseStationMessage;
import ru.java.maryan.geo_common.services.impl.KafkaMessageSender;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class EnrichmentServiceIntegrationTests {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.7.0")
    );

    private static final String TOPIC_IN = "deduplication-stations";
    private static final String TOPIC_OUT = "enrichment-stations";

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.topic-in", () -> TOPIC_IN);
        registry.add("spring.kafka.consumer.topic-out", () -> TOPIC_OUT);
        registry.add("spring.kafka.consumer.group-id", () -> "test-enrich-group");


        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JacksonJsonSerializer");


        registry.add("spring.kafka.consumer.value-deserializer", () -> "org.springframework.kafka.support.serializer.JacksonJsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");

        registry.add("celltower.towers-file", () -> "test-tower.csv");
        registry.add("celltower.tac-file", () -> "test-tac.csv");

        registry.add("logging.pattern.console",
                () -> "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{imsi}] [%thread] %logger{36} - %msg%n");
    }

    @Autowired
    private KafkaMessageSender<BaseStationMessage> sender;

    private AdminClient adminClient;

    private Consumer<String, EnrichedBaseStationMessage> testConsumer;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException, TimeoutException {
        adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()
        ));
        Set<String> topics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);

        if (topics.contains(TOPIC_IN) || topics.contains(TOPIC_OUT)) {
            adminClient.deleteTopics(List.of(TOPIC_IN, TOPIC_OUT))
                    .all().get(5, TimeUnit.SECONDS);

        }

        adminClient.createTopics(List.of(
                new NewTopic(TOPIC_IN, 1, (short) 1),
                new NewTopic(TOPIC_OUT, 1, (short) 1)
        )).all().get(5, TimeUnit.SECONDS);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JacksonJsonDeserializer<EnrichedBaseStationMessage> jsonDeserializer =
                new JacksonJsonDeserializer<>(EnrichedBaseStationMessage.class);
        jsonDeserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, EnrichedBaseStationMessage> cf = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), jsonDeserializer);

        testConsumer = cf.createConsumer();
        testConsumer.subscribe(Collections.singleton(TOPIC_OUT));
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
        if (adminClient != null) {
            adminClient.close();
        }
    }

    @Test
    void shouldEnrichAndForwardMessage() {
        BaseStationMessage validMsg = new BaseStationMessage(
                "250029999999999", "356759047890123", "79991234567",
                "7202362", "UMTS", "sys", "5301", 10L, 10L, "ATTACH", Instant.now(), -50
        );

        sender.send(validMsg, TOPIC_IN);

        ConsumerRecords<String, EnrichedBaseStationMessage> records =
                KafkaTestUtils.getRecords(testConsumer, Duration.ofSeconds(10), 1);

        assertThat(records.count()).isEqualTo(1);

        EnrichedBaseStationMessage enriched = records.iterator().next().value();

        assertThat(enriched.latitude()).isEqualTo(59.0108);
        assertThat(enriched.longitude()).isEqualTo(31.564);
        assertThat(enriched.deviceVendor()).isEqualTo("Oppo");
        assertThat(enriched.deviceModel()).isEqualTo("N1");
        System.out.printf("Received enriched message: %s", enriched);
    }

    @Test
    void shouldDropMessageIfTowerNotFound() {
        BaseStationMessage unknownTowerMsg = new BaseStationMessage(
                "250019999999999", "356759047890123", "79991234567",
                "999", "UMTS", "sys", "123", 10L, 10L, "ATTACH", Instant.now(), -50
        );

        sender.send(unknownTowerMsg, TOPIC_IN);

        ConsumerRecords<String, EnrichedBaseStationMessage> records =
                KafkaTestUtils.getRecords(testConsumer, Duration.ofSeconds(3));

        assertThat(records.isEmpty()).isTrue();
    }

    @Test
    void shouldDropMessageIfTacNotFound() {
        BaseStationMessage unknownTacMsg = new BaseStationMessage(
                "250029999999999", "909999997890123", "79991234567",
                "7202362", "UMTS", "sys", "5301", 10L, 10L, "ATTACH", Instant.now(), -50
        );

        sender.send(unknownTacMsg, TOPIC_IN);

        ConsumerRecords<String, EnrichedBaseStationMessage> records =
                KafkaTestUtils.getRecords(testConsumer, Duration.ofSeconds(3));

        assertThat(records.isEmpty()).isTrue();
    }
}