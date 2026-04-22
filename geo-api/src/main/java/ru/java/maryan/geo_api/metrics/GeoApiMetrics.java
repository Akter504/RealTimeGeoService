package ru.java.maryan.geo_api.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static ru.java.maryan.geo_api.constants.GeoApiMetricsConstants.*;

@Component
public class GeoApiMetrics {
    private final Counter profileQueryCounter;
    private final Counter radiusQueryCounter;
    private final Counter topPlacesQueryCounter;
    private final Counter notFoundErrorCounter;
    private final Counter statusQueryCounter;

    @Autowired
    public GeoApiMetrics(MeterRegistry meterRegistry) {
        this.profileQueryCounter = Counter.builder(METER_API_QUERIES)
                .tag(TAG_QUERY_TYPE, TAG_VALUE_PROFILE)
                .description(DESC_QUERIES)
                .register(meterRegistry);

        this.radiusQueryCounter = Counter.builder(METER_API_QUERIES)
                .tag(TAG_QUERY_TYPE, TAG_VALUE_RADIUS)
                .description(DESC_QUERIES)
                .register(meterRegistry);

        this.topPlacesQueryCounter = Counter.builder(METER_API_QUERIES)
                .tag(TAG_QUERY_TYPE, TAG_VALUE_TOP_PLACES)
                .description(DESC_QUERIES)
                .register(meterRegistry);

        this.notFoundErrorCounter = Counter.builder(METER_API_ERRORS)
                .tag(TAG_ERROR_TYPE, TAG_VALUE_NOT_FOUND)
                .description(DESC_ERRORS)
                .register(meterRegistry);

        this.statusQueryCounter = Counter.builder(METER_API_QUERIES)
                .tag(TAG_QUERY_TYPE, TAG_VALUE_STATUS)
                .description(DESC_QUERIES)
                .register(meterRegistry);
    }

    public void recordProfileQuery() { profileQueryCounter.increment(); }
    public void recordRadiusQuery() { radiusQueryCounter.increment(); }
    public void recordTopPlacesQuery() { topPlacesQueryCounter.increment(); }
    public void recordNotFoundError() { notFoundErrorCounter.increment(); }
    public void recordStatusQuery() {}
}