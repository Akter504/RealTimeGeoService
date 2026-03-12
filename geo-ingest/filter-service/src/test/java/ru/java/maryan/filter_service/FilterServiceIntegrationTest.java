package ru.java.maryan.filter_service;

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
import ru.java.maryan.geo_common.services.impl.KafkaMessageSender;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class FilterServiceIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.7.0")
    );
    private static final String TOPIC_IN = "from-stations";
    private static final String TOPIC_OUT = "filter-stations";

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.topic-in", () -> TOPIC_IN);
        registry.add("spring.kafka.consumer.topic-out", () -> TOPIC_OUT);
        registry.add("spring.kafka.consumer.group-id", () -> "test-filter-group");
        registry.add("spring.kafka.consumer.value-deserializer", () -> "org.springframework.kafka.support.serializer.JacksonJsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JacksonJsonSerializer");

        registry.add("logging.pattern.console",
                () -> "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{imsi}] [%thread] %logger{36} - %msg%n");
    }

    @Autowired
    private KafkaMessageSender<BaseStationMessage> sender;

    private Consumer<String, BaseStationMessage> testConsumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JacksonJsonDeserializer<BaseStationMessage> jsonDeserializer = new JacksonJsonDeserializer<>(BaseStationMessage.class);
        jsonDeserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, BaseStationMessage> cf = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), jsonDeserializer);

        testConsumer = cf.createConsumer();
        testConsumer.subscribe(Collections.singleton(TOPIC_OUT));
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    private BaseStationMessage createValidMessage() {
        return new BaseStationMessage(
                "250011234567890", // imsi (starts with 250, 15 digits)
                "351234567890123", // imei (15 digits)
                "79991234567",    // msisdn (will be cleaned to 11 digits)
                "12345",           // cellId (digits)
                "LTE",              // rat (Assuming valid in RatType)
                "sys-1",
                "67890",           // lac (digits)
                1024L,                  // volumeBytes
                60L,                    // durationSec
                "ATTACH",            // eventType (Assuming valid in RelevantEventType)
                Instant.now(),     // timestamp (now)
                -70                // signalStrength (Assuming valid for 4G)
        );
    }

    @Test
    void shouldProcessAndForwardValidMessage() {
        BaseStationMessage validMsg = createValidMessage();

        sender.send(validMsg, TOPIC_IN);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            ConsumerRecords<String, BaseStationMessage> records = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(100));
            assertThat(records.count()).isEqualTo(1);
            assertThat(records.iterator().next().value().imsi()).isEqualTo("250011234567890");
        });
    }

    @Test
    void shouldDropInvalidMessage() {
        BaseStationMessage invalidMsg = new BaseStationMessage(
                "999999999999999", "351234567890123", "79991234567",
                "123", "4G", "sys", "456", 10L, 10L, "CALL", Instant.now(), -50
        );

        sender.send(invalidMsg, TOPIC_IN);

        await().pollDelay(Duration.ofSeconds(3)).untilAsserted(() -> {
            ConsumerRecords<String, BaseStationMessage> records = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(100));
            assertThat(records.isEmpty()).isTrue();
        });
    }
}
