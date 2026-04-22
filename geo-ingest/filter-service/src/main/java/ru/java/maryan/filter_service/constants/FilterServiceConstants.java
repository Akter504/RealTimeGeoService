package ru.java.maryan.filter_service.constants;

public final class FilterServiceConstants {
    private FilterServiceConstants() {}

    public static final String METER_GEO_EVENTS_RECEIVED = "geo_events_received_total";
    public static final String METER_GEO_EVENTS_PROCESSED = "geo_events_processed_total";
    public static final String TAG_STATUS = "status";
    public static final String TAG_VALUE_RECEIVED = "received";
    public static final String TAG_VALUE_PASSED = "passed";
    public static final String TAG_VALUE_DROPPED = "dropped";
    public static final String DESC_RECEIVED = "Total number of raw events received from base stations";
    public static final String DESC_PASSED = "Events that passed validation";
    public static final String DESC_DROPPED = "Events dropped by validation rules";

}
