package ru.java.maryan.deduplication_service.constants;

public final class DeduplicationServiceConstants {
    private DeduplicationServiceConstants() {}

    public static final String METER_GEO_EVENTS_RECEIVED = "geo_events_dedup_received_total";
    public static final String METER_GEO_EVENTS_PROCESSED = "geo_events_dedup_processed_total";

    public static final String TAG_RESULT = "result";
    public static final String TAG_VALUE_RECEIVED = "received";
    public static final String TAG_VALUE_UNIQUE = "unique";
    public static final String TAG_VALUE_DUPLICATE = "duplicate";

    public static final String DESC_RECEIVED = "Total number of events received for deduplication";
    public static final String DESC_UNIQUE = "Events that were successfully processed as unique";
    public static final String DESC_DUPLICATE = "Events dropped as duplicates";
}
