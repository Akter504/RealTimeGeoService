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

CREATE TABLE IF NOT EXISTS kafka_enriched_events_queue (
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
) ENGINE = Kafka
SETTINGS
    kafka_broker_list = 'kafka:9092',
    kafka_topic_list = 'enrichment-stations',
    kafka_group_name = 'clickhouse_reader_grp',
    kafka_format = 'JSONEachRow',
    kafka_max_block_size = 1048576;

CREATE MATERIALIZED VIEW IF NOT EXISTS enriched_events_mv TO enriched_geo_events AS
SELECT * FROM kafka_enriched_events_queue;
