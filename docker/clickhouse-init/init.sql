CREATE TABLE IF NOT EXISTS enriched_geo_events (
    msisdn String,
    imsi String,
    imei String,
    mcc String,
    mnc String,
    lac String,
    rat String,
    rawCellId UInt64,
    latitude Float64,
    longitude Float64,
    deviceVendor String,
    deviceModel String,
    timestamp DateTime64(3, 'UTC'),
    durationSec UInt64,
    signalStrength Int32
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(timestamp)
ORDER BY (imsi, timestamp);

CREATE TABLE kafka_enriched_events_queue (
                                             msisdn String,
                                             imsi String,
                                             imei String,
                                             mcc String,
                                             mnc String,
                                             lac String,
                                             rat String,
                                             rawCellId UInt64,
                                             latitude Float64,
                                             longitude Float64,
                                             deviceVendor String,
                                             deviceModel String,
                                             timestamp String,
                                             durationSec UInt64,
                                             signalStrength Int32
) ENGINE = Kafka
SETTINGS
    kafka_broker_list = 'kafka:29092',
    kafka_topic_list = 'enrichment-stations',
    kafka_group_name = 'clickhouse_final_fix_group', -- Новая группа, чтобы прочитать старое
    kafka_format = 'JSONEachRow';

CREATE MATERIALIZED VIEW enriched_events_mv TO enriched_geo_events AS
SELECT
    msisdn, imsi, imei, mcc, mnc, lac, rat,
    toUInt64(rawCellId) AS rawCellId,
    latitude, longitude, deviceVendor, deviceModel,
    toDateTime64(replaceAll(replaceRegexpOne(timestamp, 'Z$', ''), 'T', ' '), 3) AS timestamp,
    durationSec, signalStrength
FROM kafka_enriched_events_queue;
