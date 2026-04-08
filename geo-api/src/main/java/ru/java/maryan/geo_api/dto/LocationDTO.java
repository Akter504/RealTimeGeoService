package ru.java.maryan.geo_api.dto;

import java.time.Instant;

public record LocationDTO(
        String lac,
        String cellId,
        Double lat,
        Double lon,
        Instant timestamp,
        Integer status // 0 - Other, 1 - Work, 2 - Home
) {}
