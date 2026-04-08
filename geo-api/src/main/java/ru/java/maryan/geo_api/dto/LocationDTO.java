package ru.java.maryan.geo_api.dto;

import ru.java.maryan.geo_api.enums.SubscriberStatus;

import java.time.Instant;

public record LocationDTO(
        String lac,
        String cellId,
        Double lat,
        Double lon,
        Instant timestamp,
        SubscriberStatus status
) {}
