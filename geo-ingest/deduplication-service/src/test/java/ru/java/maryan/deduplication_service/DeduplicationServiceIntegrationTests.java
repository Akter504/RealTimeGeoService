package ru.java.maryan.deduplication_service;

import com.redis.testcontainers.RedisContainer;
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
import org.springframework.data.redis.core.StringRedisTemplate;
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
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class DeduplicationServiceIntegrationTests {

    @Container
    static final RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:7-alpine")
    );

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.7.0")
    );
    private static final String TOPIC_IN = "filter-stations";
    private static final String TOPIC_OUT = "deduplication-stations";

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.topic-in", () -> TOPIC_IN);
        registry.add("spring.kafka.consumer.topic-out", () -> TOPIC_OUT);
        registry.add("spring.kafka.consumer.group-id", () -> "test-filter-group");
        registry.add("spring.kafka.consumer.value-deserializer", () -> "org.springframework.kafka.support.serializer.JacksonJsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JacksonJsonSerializer");

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        registry.add("spring.redis.ttl", () -> "10m");

        registry.add("logging.pattern.console",
                () -> "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{imsi}] [%thread] %logger{36} - %msg%n");
    }

    @Autowired
    private KafkaMessageSender<BaseStationMessage> sender;

    private Consumer<String, BaseStationMessage> testConsumer;

    private AdminClient adminClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException, TimeoutException {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
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
        if (adminClient != null) {
            adminClient.close();
        }
    }

    @Test
    void shouldProcessTwoUniqueMessages() {
        BaseStationMessage msgOne = new BaseStationMessage(
                "111111111111111", "351234567890123", "79991234567",
                "123", "LTE", "sys", "456", 10L, 10L, "ATTACH", Instant.now(), -50
        );

        BaseStationMessage msgTwo = new BaseStationMessage(
                "222222222222222", "351234567890999", "79991234567",
                "999", "LTE", "sys", "888", 10L, 10L, "ATTACH", Instant.now(), -50
        );

        sender.send(msgOne, TOPIC_IN);
        sender.send(msgTwo, TOPIC_IN);

        ConsumerRecords<String, BaseStationMessage> records =
                KafkaTestUtils.getRecords(testConsumer, Duration.ofSeconds(10), 2);

        assertThat(records.count()).isEqualTo(2);

        int keysForMsgOne = redisTemplate.keys("*" + msgOne.imsi() + "*").size();
        int keysForMsgTwo = redisTemplate.keys("*" + msgTwo.imsi() + "*").size();

        assertThat(keysForMsgOne).isEqualTo(1);
        assertThat(keysForMsgTwo).isEqualTo(1);
    }

    @Test
    void shouldFilterOutDuplicateMessage() {
        BaseStationMessage original = new BaseStationMessage(
                "250011234555555", "351234567890999", "79991234567",
                "123", "LTE", "sys", "456", 10L, 10L, "ATTACH", Instant.now(), -50
        );

        BaseStationMessage duplicate = new BaseStationMessage(
                "250011234555555", "351234567890999", "79991234567",
                "123", "LTE", "sys", "456", 10L, 10L, "ATTACH", Instant.now(), -50
        );

        sender.send(original, TOPIC_IN);

        ConsumerRecords<String, BaseStationMessage> firstRecords =
                KafkaTestUtils.getRecords(testConsumer, Duration.ofSeconds(10), 1);
        assertThat(firstRecords.count()).isEqualTo(1);

        sender.send(duplicate, TOPIC_IN);

        ConsumerRecords<String, BaseStationMessage> secondRecords =
                KafkaTestUtils.getRecords(testConsumer, Duration.ofSeconds(3));

        assertThat(secondRecords.isEmpty()).isTrue();

        int keyForOriginal = redisTemplate.keys("*" + original.imsi() + "*").size();

        assertThat(keyForOriginal).isEqualTo(1);
    }
}
