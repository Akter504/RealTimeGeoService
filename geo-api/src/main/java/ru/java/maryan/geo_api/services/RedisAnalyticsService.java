package ru.java.maryan.geo_api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.java.maryan.geo_api.dto.LocationDTO;
import ru.java.maryan.geo_api.records.LastKnownLocationRecord;

import static ru.java.maryan.geo_api.constants.RedisConstant.*;

@Slf4j
@Service
public class RedisAnalyticsService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public RedisAnalyticsService(StringRedisTemplate redisTemplate,
                                 ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean subscriberExists(String imsi) {
        String redisKey = LAST_KNOWN_LOCATION_PREFIX + COLON + imsi;
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }

    public LocationDTO getLastKnownLocation(String imsi) {
        String redisKey = LAST_KNOWN_LOCATION_PREFIX + COLON + imsi;
        String json = redisTemplate.opsForValue().get(redisKey);

        if (json == null) {
            return null;
        }

        try {
            LastKnownLocationRecord lkl = objectMapper.readValue(json, LastKnownLocationRecord.class);

            return new LocationDTO(
                    lkl.lac(),
                    lkl.cellId(),
                    lkl.lat(),
                    lkl.lon(),
                    lkl.timestamp(),
                    lkl.status()
            );
        } catch (Exception e) {
            log.error("Failed to parse LKL for IMSI {}", imsi, e);
            return null;
        }
    }

    public Integer countUsersInRadius(double lat, double lon, double radiusMeters) {
        Circle searchArea = new Circle(
                new Point(lon, lat),
                new Distance(radiusMeters / 1000.0, Metrics.KILOMETERS)
        );

        var results = redisTemplate.opsForGeo().radius(GEO_GLOBAL_MAP_KEY, searchArea);

        if (results == null) return 0;
        return results.getContent().size();
    }

    public Integer getSubscriberStatus(String imsi) {
        String redisKey = LAST_KNOWN_LOCATION_PREFIX + COLON + imsi;
        String json = redisTemplate.opsForValue().get(redisKey);

        if (json == null) {
            return null;
        }

        try {
            LastKnownLocationRecord lkl = objectMapper.readValue(json, LastKnownLocationRecord.class);
            return lkl.status();
        } catch (Exception e) {
            log.error("Failed to parse LKL status for IMSI {}", imsi, e);
            return null;
        }
    }

}
