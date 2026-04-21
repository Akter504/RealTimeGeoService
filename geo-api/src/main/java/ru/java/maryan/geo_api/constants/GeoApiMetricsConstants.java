package ru.java.maryan.geo_api.constants;

public final class GeoApiMetricsConstants {
    private GeoApiMetricsConstants() {}

    public static final String METER_API_QUERIES = "geo_api_queries_total";
    public static final String METER_API_ERRORS = "geo_api_errors_total";

    public static final String TAG_QUERY_TYPE = "query_type";
    public static final String TAG_ERROR_TYPE = "error_type";

    public static final String TAG_VALUE_PROFILE = "profile";
    public static final String TAG_VALUE_RADIUS = "radius";
    public static final String TAG_VALUE_TOP_PLACES = "top_places";
    public static final String TAG_VALUE_NOT_FOUND = "not_found";
    public static final String TAG_VALUE_STATUS = "status";

    public static final String DESC_QUERIES = "Total number of GraphQL queries executed";
    public static final String DESC_ERRORS = "Total number of API business errors";
}
