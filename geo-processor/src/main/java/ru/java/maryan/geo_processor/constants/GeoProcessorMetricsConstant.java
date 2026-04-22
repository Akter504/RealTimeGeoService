package ru.java.maryan.geo_processor.constants;

public final class GeoProcessorMetricsConstant {
    private GeoProcessorMetricsConstant() {}

    public static final String METER_PROCESSOR_RECEIVED = "geo_processor_received_total";
    public static final String METER_TRIGGERS_GENERATED = "geo_triggers_generated_total";
    public static final String METER_PROFILE_CACHE = "geo_profile_cache_total";

    public static final String TAG_TYPE = "type";
    public static final String TAG_RESULT = "result";

    public static final String TAG_VALUE_RECEIVED = "received";
    public static final String TAG_VALUE_ENTRY = "entry";
    public static final String TAG_VALUE_EXIT = "exit";
    public static final String TAG_VALUE_HIT = "hit";
    public static final String TAG_VALUE_MISS = "miss";

    public static final String DESC_RECEIVED = "Total number of enriched events received by processor";
    public static final String DESC_TRIGGERS = "Total number of edge-triggered events (ENTRY/EXIT)";
    public static final String DESC_CACHE = "Cache hit/miss ratio for baseline profiles";

}
