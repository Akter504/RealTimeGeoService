package ru.java.maryan.geo_api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import ru.java.maryan.geo_api.dto.PlaceStatDTO;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static ru.java.maryan.geo_api.constants.ClickHouseConstant.*;

@Slf4j
@Service
public class ClickHouseAnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    @Value("classpath:analytics/click-house/top_places.sql")
    private Resource topPlacesResource;

    private String sqlTopPlaces;

    @Autowired
    public ClickHouseAnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initSqlQueries() {
        try {
            sqlTopPlaces = StreamUtils.copyToString(topPlacesResource.getInputStream(), StandardCharsets.UTF_8);
            log.info("Analytics SQL queries successfully loaded into memory.");
        } catch (IOException e) {
            log.error("Failed to load SQL files from classpath", e);
            throw new RuntimeException("Cannot start service without SQL files", e);
        }
    }

    public List<PlaceStatDTO> getTopPlaces(String imsi, int limit) {
        log.debug("Executing Top Places query for IMSI: {}", imsi);
        try {
            return jdbcTemplate.query(
                    sqlTopPlaces,
                    (rs, rowNum) -> new PlaceStatDTO(rs.getString(CELL_ID),
                            rs.getInt(VISITS_COUNT)),
                    imsi, limit
            );
        } catch (Exception e) {
            log.error("ClickHouse error for Top Places", e);
            return List.of();
        }
    }
}