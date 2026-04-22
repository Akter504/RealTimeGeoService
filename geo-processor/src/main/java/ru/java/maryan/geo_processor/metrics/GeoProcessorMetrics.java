package ru.java.maryan.geo_processor.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static ru.java.maryan.geo_processor.constants.GeoProcessorMetricsConstant.*;

@Component
public class GeoProcessorMetrics {
    private final Counter receivedCounter;
    private final Counter entryTriggerCounter;
    private final Counter exitTriggerCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    @Autowired
    public GeoProcessorMetrics(MeterRegistry meterRegistry) {
        this.receivedCounter = Counter.builder(METER_PROCESSOR_RECEIVED)
                .tag(TAG_TYPE, TAG_VALUE_RECEIVED)
                .description(DESC_RECEIVED)
                .register(meterRegistry);

        this.entryTriggerCounter = Counter.builder(METER_TRIGGERS_GENERATED)
                .tag(TAG_TYPE, TAG_VALUE_ENTRY)
                .description(DESC_TRIGGERS)
                .register(meterRegistry);

        this.exitTriggerCounter = Counter.builder(METER_TRIGGERS_GENERATED)
                .tag(TAG_TYPE, TAG_VALUE_EXIT)
                .description(DESC_TRIGGERS)
                .register(meterRegistry);

        this.cacheHitCounter = Counter.builder(METER_PROFILE_CACHE)
                .tag(TAG_RESULT, TAG_VALUE_HIT)
                .description(DESC_CACHE)
                .register(meterRegistry);

        this.cacheMissCounter = Counter.builder(METER_PROFILE_CACHE)
                .tag(TAG_RESULT, TAG_VALUE_MISS)
                .description(DESC_CACHE)
                .register(meterRegistry);
    }

    public void recordReceived() { receivedCounter.increment(); }
    public void recordEntryTrigger() { entryTriggerCounter.increment(); }
    public void recordExitTrigger() { exitTriggerCounter.increment(); }
    public void recordCacheHit() { cacheHitCounter.increment(); }
    public void recordCacheMiss() { cacheMissCounter.increment(); }
}