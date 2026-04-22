package ru.java.maryan.geo_processor.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import ru.java.maryan.geo_processor.metrics.GeoProcessorMetrics;
import ru.java.maryan.geo_processor.records.CellLocationRecord;
import ru.java.maryan.geo_processor.records.SubscriberProfileRecord;

import static ru.java.maryan.geo_processor.constants.ClickHouseConstant.*;
import static ru.java.maryan.geo_processor.constants.GeoProcessorConstants.*;

@Slf4j
@Service
public class HomeWorkStatusService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final GeoProcessorMetrics processorMetrics;
    private final ObjectMapper mapper;
    private final String homeSql;
    private final String workSql;

    public HomeWorkStatusService(
            @Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate,
            GeoProcessorMetrics processorMetrics,
            @Value("classpath:analytics/click-house/home_location.sql") Resource homeSqlResource,
            @Value("classpath:analytics/click-house/work_location.sql") Resource workSqlResource,
            StringRedisTemplate redisTemplate,
            ObjectMapper mapper
    ) throws IOException {
        this.jdbcTemplate = jdbcTemplate;
        this.processorMetrics = processorMetrics;
        this.redisTemplate = redisTemplate;
        this.mapper = mapper;
        this.homeSql = homeSqlResource.getContentAsString(StandardCharsets.UTF_8);
        this.workSql = workSqlResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public int resolveStatus(String imsi, String lac, String cellId) {
        SubscriberProfileRecord profile = getProfileFromCacheOrCalculate(imsi);

        if (profile == null) {
            return STATUS_OTHER;
        }

        if (profile.home() != null && profile.home().matches(lac, cellId)) {
            return STATUS_HOME;
        }
        if (profile.work() != null && profile.work().matches(lac, cellId)) {
            return STATUS_WORK;
        }

        return STATUS_OTHER;
    }

    private SubscriberProfileRecord getProfileFromCacheOrCalculate(String imsi) {
        String redisKey = PROFILE_KEY_PREFIX + imsi;
        String cachedJson = redisTemplate.opsForValue().get(redisKey);

        if (cachedJson != null) {
            try {
                processorMetrics.recordCacheHit();
                return mapper.readValue(cachedJson, SubscriberProfileRecord.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse cached profile for IMSI {}", imsi, e);
            }
        }

        log.debug("Cache miss for {}. Calculating profile in ClickHouse...", imsi);
        processorMetrics.recordCacheMiss();

        CellLocationRecord home = findTopLocation(homeSql, imsi);
        CellLocationRecord work = findTopLocation(workSql, imsi);
        SubscriberProfileRecord profile = new SubscriberProfileRecord(home, work);

        try {
            redisTemplate.opsForValue().set(redisKey, mapper.writeValueAsString(profile), PROFILE_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache profile for IMSI {}", imsi, e);
        }

        return profile;
    }

    private CellLocationRecord findTopLocation(String sql, String imsi) {
        try {
            List<CellLocationRecord> rows = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new CellLocationRecord(rs.getString(LAC), rs.getString(CELL_ID)),
                    imsi
            );
            return rows.isEmpty() ? null : rows.getFirst();
        } catch (Exception e) {
            log.warn("Cannot load profile from ClickHouse for IMSI {}", imsi, e);
            return null;
        }
    }

}


