package ru.java.maryan.enrichment_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static ru.java.maryan.enrichment_service.constants.EnrichmentServiceConstants.*;

@Component
public class EnrichmentServiceMetrics {
    private final Counter receivedCounter;
    private final Counter enrichedCounter;
    private final Counter postponedCounter;
    private final Counter droppedCounter;

    @Autowired
    public EnrichmentServiceMetrics(MeterRegistry meterRegistry) {
        this.receivedCounter = Counter.builder(METER_GEO_EVENTS_RECEIVED)
                .tag(TAG_STATUS, TAG_VALUE_RECEIVED)
                .description(DESC_RECEIVED)
                .register(meterRegistry);

        this.enrichedCounter = Counter.builder(METER_GEO_EVENTS_PROCESSED)
                .tag(TAG_STATUS, TAG_VALUE_ENRICHED)
                .description(DESC_ENRICHED)
                .register(meterRegistry);

        this.postponedCounter = Counter.builder(METER_GEO_EVENTS_PROCESSED)
                .tag(TAG_STATUS, TAG_VALUE_POSTPONED)
                .description(DESC_POSTPONED)
                .register(meterRegistry);

        this.droppedCounter = Counter.builder(METER_GEO_EVENTS_PROCESSED)
                .tag(TAG_STATUS, TAG_VALUE_DROPPED)
                .description(DESC_DROPPED)
                .register(meterRegistry);
    }

    public void recordReceived() { receivedCounter.increment(); }
    public void recordEnriched() { enrichedCounter.increment(); }
    public void recordPostponed() { postponedCounter.increment(); }
    public void recordDropped() { droppedCounter.increment(); }
}