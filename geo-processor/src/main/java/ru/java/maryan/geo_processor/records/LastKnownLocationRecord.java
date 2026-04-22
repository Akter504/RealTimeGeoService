package ru.java.maryan.geo_processor.records;

import java.time.Instant;

public record LastKnownLocationRecord(
        String cellId,
        String lac,
        Instant timestamp,
        Double lat,
        Double lon,
        Integer status // 0 - Other, 1 - Work, 2 - Home
) {
}
