package ru.java.maryan.geo_processor.dto;

import java.time.Instant;

public record LocationTriggerEvent(
        String subscriberId,
        String eventType,
        String lac,
        String cellId,
        Instant timestamp
) {
}
