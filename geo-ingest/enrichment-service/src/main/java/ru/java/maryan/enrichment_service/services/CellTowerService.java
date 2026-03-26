package ru.java.maryan.enrichment_service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.java.maryan.enrichment_service.records.TacCsvRecord;
import ru.java.maryan.enrichment_service.records.TowerCsvRecord;


@Service
public class CellTowerService {
    private static final Logger log = LoggerFactory.getLogger(CellTowerService.class);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public CellTowerService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T find(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Error parsing {} from Redis for key: {}",
                    clazz.getSimpleName(), key, e);
            return null;
        }
    }

    public TowerCsvRecord findTower(String key) {
        return find(key, TowerCsvRecord.class);
    }

    public TacCsvRecord findTac(String key) {
        return find(key, TacCsvRecord.class);
    }

    public String findUnicalId(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}