package ru.java.maryan.enrichment_service.constants;

public final class EnrichmentServiceConstants {
    private EnrichmentServiceConstants() {}

    public static final String METER_GEO_EVENTS_RECEIVED = "geo_events_enrich_received_total";
    public static final String METER_GEO_EVENTS_PROCESSED = "geo_events_enrich_processed_total";

    public static final String TAG_STATUS = "status";
    public static final String TAG_VALUE_RECEIVED = "received";
    public static final String TAG_VALUE_ENRICHED = "enriched";
    public static final String TAG_VALUE_POSTPONED = "postponed";
    public static final String TAG_VALUE_DROPPED = "dropped";

    public static final String DESC_RECEIVED = "Total number of events received for enrichment";
    public static final String DESC_ENRICHED = "Events successfully enriched with coordinates and device data";
    public static final String DESC_POSTPONED = "Events sent to delayed queue due to missing dictionary data";
    public static final String DESC_DROPPED = "Events completely dropped (e.g. missing identity)";

    public static final String BATCH_DELAYED_ENRICHMENT = "BATCH:DELAYED_ENRICHMENT";
}
