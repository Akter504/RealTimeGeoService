package ru.java.maryan.geo_processor.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;

import static ru.java.maryan.geo_processor.constants.ClickHouseConstant.*;
import static ru.java.maryan.geo_processor.constants.GeoProcessorConstants.STATUS_HOME;
import static ru.java.maryan.geo_processor.constants.GeoProcessorConstants.STATUS_OTHER;
import static ru.java.maryan.geo_processor.constants.GeoProcessorConstants.STATUS_WORK;

@Slf4j
@Service
public class HomeWorkStatusService {

    private final JdbcTemplate jdbcTemplate;
    private final String homeSql;
    private final String workSql;

    public HomeWorkStatusService(
            @Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Value("classpath:analytics/click-house/home_location.sql") Resource homeSqlResource,
            @Value("classpath:analytics/click-house/work_location.sql") Resource workSqlResource
    ) throws IOException {
        this.jdbcTemplate = jdbcTemplate;
        this.homeSql = homeSqlResource.getContentAsString(StandardCharsets.UTF_8);
        this.workSql = workSqlResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public int resolveStatus(String imsi, String lac, String cellId) {
        CellLocation home = findTopLocation(homeSql, imsi);
        if (home != null && home.matches(lac, cellId)) {
            return STATUS_HOME;
        }

        CellLocation work = findTopLocation(workSql, imsi);
        if (work != null && work.matches(lac, cellId)) {
            return STATUS_WORK;
        }

        return STATUS_OTHER;
    }

    private CellLocation findTopLocation(String sql, String imsi) {
        try {
            List<CellLocation> rows = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new CellLocation(rs.getString(LAC), rs.getString(CELL_ID)),
                    imsi
            );
            return rows.isEmpty() ? null : rows.getFirst();
        } catch (Exception e) {
            log.warn("Cannot load profile from ClickHouse for IMSI {}", imsi, e);
            return null;
        }
    }

    private record CellLocation(String lac, String cellId) {
        private boolean matches(String lac, String cellId) {
            return this.lac.equals(lac) && this.cellId.equals(cellId);
        }
    }
}


