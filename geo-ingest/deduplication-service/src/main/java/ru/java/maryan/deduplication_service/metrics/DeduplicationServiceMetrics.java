package ru.java.maryan.deduplication_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static ru.java.maryan.deduplication_service.constants.DeduplicationServiceConstants.*;

@Component
public class DeduplicationServiceMetrics {
    private final Counter receivedCounter;
    private final Counter uniqueCounter;
    private final Counter duplicateCounter;

    @Autowired
    public DeduplicationServiceMetrics(MeterRegistry meterRegistry) {
        this.receivedCounter = Counter.builder(METER_GEO_EVENTS_RECEIVED)
                .tag(TAG_RESULT, TAG_VALUE_RECEIVED)
                .description(DESC_RECEIVED)
                .register(meterRegistry);

        this.uniqueCounter = Counter.builder(METER_GEO_EVENTS_PROCESSED)
                .tag(TAG_RESULT, TAG_VALUE_UNIQUE)
                .description(DESC_UNIQUE)
                .register(meterRegistry);

        this.duplicateCounter = Counter.builder(METER_GEO_EVENTS_PROCESSED)
                .tag(TAG_RESULT, TAG_VALUE_DUPLICATE)
                .description(DESC_DUPLICATE)
                .register(meterRegistry);
    }

    public void recordReceived() { receivedCounter.increment(); }
    public void recordUnique() { uniqueCounter.increment(); }
    public void recordDuplicate() { duplicateCounter.increment(); }
}
