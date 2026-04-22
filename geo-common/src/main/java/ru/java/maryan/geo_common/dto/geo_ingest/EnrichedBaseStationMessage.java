package ru.java.maryan.geo_common.dto.geo_ingest;

import java.time.Instant;

public record EnrichedBaseStationMessage(
        String msisdn,
        String imsi,
        String imei,

        String mcc,
        String mnc,
        String lac,
        String rat,

        Long rawCellId,
//        Long towerId,
//        Integer sectorId,

        Double latitude,
        Double longitude,
//        Integer azimuth,

//        Long h3Index,

        //String regionCode, // На подумать (микро-регионы)

        String deviceVendor,
        String deviceModel,

        Instant timestamp,
        Long durationSec,
        Integer signalStrength)
{
}
