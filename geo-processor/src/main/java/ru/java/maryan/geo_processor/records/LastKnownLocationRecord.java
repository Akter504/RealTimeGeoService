package ru.java.maryan.geo_processor.records;

import java.time.Instant;

public record LastKnownLocationRecord(
        String cellId,
        String lac,
        Instant timestamp
) {
}
