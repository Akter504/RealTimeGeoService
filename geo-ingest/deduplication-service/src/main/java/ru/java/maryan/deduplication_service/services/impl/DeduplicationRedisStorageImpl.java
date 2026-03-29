package ru.java.maryan.deduplication_service.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import ru.java.maryan.deduplication_service.services.DeduplicationRedisStorage;
import ru.java.maryan.geo_common.dto.geo_ingest.BaseStationMessage;

import java.time.Duration;

import static ru.java.maryan.geo_common.constants.StationMessage.COLON;
import static ru.java.maryan.geo_common.constants.StationMessage.MSISDN;

@Slf4j
@Component
public class DeduplicationRedisStorageImpl implements DeduplicationRedisStorage<BaseStationMessage> {

    private final StringRedisTemplate redisTemplate;

    @Value("${spring.redis.ttl}")
    private Duration ttl;

    @Autowired
    public DeduplicationRedisStorageImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String KEY_FORMAT = "dedupe:%s:%s:%s";

    @Override
    public boolean save(BaseStationMessage message) {
        String key = String.format(KEY_FORMAT,
                resolveUniversalId(message),
                message.cellId(),
                message.lac()
        );

        if (isDuplicate(key)) {
            log.debug("Key {} already exists in Redis (Timestamp: {})", key, message.timestamp());
            return false;
        }
        redisTemplate.opsForValue().set(key, key, ttl);
        log.debug("Saved new key to Redis: {}", key);
        return true;
    }

    private String resolveUniversalId(BaseStationMessage msg) {
        String imsi = msg.imsi();
        if (imsi != null && !imsi.isBlank()) {
            return imsi;
        }

        String msisdn = msg.msisdn();
        imsi = redisTemplate.opsForValue().get(MSISDN + COLON + msisdn);
        if (imsi != null) {
            log.debug("Resolved MSISDN {} to IMSI {}", msisdn, imsi);
            return imsi;
        }
        log.warn("Not found IMSI from dict, MSISDN: {}", msisdn);
        return msisdn;
    }

    private boolean isDuplicate(String key) {
        return redisTemplate.opsForValue().get(key) != null;
    }
}
