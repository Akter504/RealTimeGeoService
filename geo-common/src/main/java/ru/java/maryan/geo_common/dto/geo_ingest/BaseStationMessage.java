package ru.java.maryan.geo_common.dto.geo_ingest;

import java.time.Instant;

public record BaseStationMessage(
        String imsi,
        String imei,
        String msisdn,
        String cellId,
        String rat,
        String sourceSystem,
        String lac,
        Long volumeBytes,
        Long durationSec,
        String eventType,
        Instant timestamp,
        Integer signalStrength
) {
}