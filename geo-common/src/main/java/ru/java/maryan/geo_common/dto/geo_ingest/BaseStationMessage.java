package ru.java.maryan.geo_common.dto.geo_ingest;

import java.time.Instant;

import static ru.java.maryan.geo_common.constants.KafkaConstants.IMSI_NULL_STATUS;
import static ru.java.maryan.geo_common.constants.StationMessage.*;

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
    public String getTraceId() {
        if (this.imsi() != null && !this.imsi().isBlank()) {
            return IMSI + COLON + this.imsi();
        }
        if (this.msisdn() != null && !this.msisdn().isBlank()) {
            return MSISDN + COLON + this.msisdn();
        }
        return IMSI_NULL_STATUS;
    }

}