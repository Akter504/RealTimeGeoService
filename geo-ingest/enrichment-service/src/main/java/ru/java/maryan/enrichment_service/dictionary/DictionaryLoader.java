package ru.java.maryan.enrichment_service.dictionary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.java.maryan.enrichment_service.records.SubscriberCsvRecord;
import ru.java.maryan.enrichment_service.records.TacCsvRecord;
import ru.java.maryan.enrichment_service.records.TowerCsvRecord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import static ru.java.maryan.geo_common.constants.StationMessage.*;

@Slf4j
@Service
public class DictionaryLoader {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${celltower.towers-file:RussiaCells.csv}")
    private String towersFileName;

    @Value("${celltower.tac-file:tacdb.csv}")
    private String tacFileName;

    @Value("${celltower.subscribers-file:subscribers.csv}")
    private String subscribersFileName;

    @Value("${app.init-redis-dicts:false}")
    private boolean initDicts;

    @Autowired
    public DictionaryLoader(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (!initDicts) {
            log.info("Redis dictionary initialization is disabled.");
            return;
        }
        loadTowersToRedis();
        loadTacToRedis();
        loadSubscribersToRedis();
    }

    private void loadTowersToRedis() {
        log.info("Starting to load towers from CSV to Redis...");
        long start = System.currentTimeMillis();
        try {
            ClassPathResource resource = new ClassPathResource(towersFileName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {

                List<TowerCsvRecord> records = new CsvToBeanBuilder<TowerCsvRecord>(reader)
                        .withType(TowerCsvRecord.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withSeparator(',')
                        .build()
                        .parse();

                int count = 0;
                for (TowerCsvRecord towerCsvRecord : records) {
                    String redisKey = towerCsvRecord.getUniqueKey();
                    String jsonValue = objectMapper.writeValueAsString(towerCsvRecord);

                    redisTemplate.opsForValue().set(redisKey, jsonValue);
                    count++;
                }

                log.info("Successfully loaded {} towers to Redis in {} ms", count, System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            log.error("Failed to load towers to Redis", e);
        }
    }

    private void loadTacToRedis() {
        log.info("Starting to load TAC from CSV to Redis...");
        long start = System.currentTimeMillis();
        try {
            ClassPathResource resource = new ClassPathResource(tacFileName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {

                List<TacCsvRecord> records = new CsvToBeanBuilder<TacCsvRecord>(reader)
                        .withType(TacCsvRecord.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withSeparator(',')
                        .build()
                        .parse();

                int count = 0;
                for (TacCsvRecord tacCsvRecord : records) {
                    String redisKey = tacCsvRecord.getTac().toString();
                    String jsonValue = objectMapper.writeValueAsString(tacCsvRecord);

                    redisTemplate.opsForValue().set(TAC + COLON + redisKey, jsonValue);
                    count++;
                }

                log.info("Successfully loaded {} tac to Redis in {} ms", count, System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            log.error("Failed to load tac to Redis", e);
        }
    }

    private void loadSubscribersToRedis() {
        log.info("Starting to load Subscribers from CSV to Redis...");
        long start = System.currentTimeMillis();
        try {
            ClassPathResource resource = new ClassPathResource(subscribersFileName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {

                List<SubscriberCsvRecord> records = new CsvToBeanBuilder<SubscriberCsvRecord>(reader)
                        .withType(SubscriberCsvRecord.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withSeparator(',')
                        .build()
                        .parse();

                int count = 0;
                for (SubscriberCsvRecord subscriberCsvRecord : records) {
                    String imsi = subscriberCsvRecord.getImsi();
                    String msisdn = subscriberCsvRecord.getMsisdn();

                    redisTemplate.opsForValue().set(IMSI + COLON + imsi, msisdn);
                    redisTemplate.opsForValue().set(MSISDN + COLON + msisdn, imsi);
                    count++;
                }

                log.info("Successfully loaded {} subscribers to Redis in {} ms", count, System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            log.error("Failed to load tac to Redis", e);
        }
    }
}