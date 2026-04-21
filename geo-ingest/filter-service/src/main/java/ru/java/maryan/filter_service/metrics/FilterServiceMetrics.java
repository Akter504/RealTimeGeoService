package ru.java.maryan.filter_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static ru.java.maryan.filter_service.constants.FilterServiceConstants.*;

@Component
public class FilterServiceMetrics {
    private final Counter receivedCounter;
    private final Counter passedCounter;
    private final Counter droppedCounter;

    @Autowired
    public FilterServiceMetrics(MeterRegistry meterRegistry) {
        this.receivedCounter = Counter.builder(METER_GEO_EVENTS_RECEIVED)
                .tag(TAG_STATUS, TAG_VALUE_RECEIVED)
                .description(DESC_RECEIVED)
                .register(meterRegistry);

        this.passedCounter = Counter.builder(METER_GEO_EVENTS_PROCESSED)
                .tag(TAG_STATUS, TAG_VALUE_PASSED)
                .description(DESC_PASSED)
                .register(meterRegistry);

        this.droppedCounter = Counter.builder(METER_GEO_EVENTS_PROCESSED)
                .tag(TAG_STATUS, TAG_VALUE_DROPPED)
                .description(DESC_DROPPED)
                .register(meterRegistry);
    }

    public void recordReceived() { receivedCounter.increment(); }
    public void recordPassed() { passedCounter.increment(); }
    public void recordDropped() { droppedCounter.increment(); }
}
