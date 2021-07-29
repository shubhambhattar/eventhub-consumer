package com.example.demo.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class ConsumerMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicLong> lags;
    private final Map<String, AtomicLong> eventTimestamp;

    public ConsumerMetrics(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        lags = new ConcurrentHashMap<>();
        eventTimestamp = new ConcurrentHashMap<>();
    }

    @Bean
    public ConsumerMetrics getConsumerMetrics(final MeterRegistry meterRegistry) {
        return new ConsumerMetrics(meterRegistry);
    }

    public void countEvents(final String partitionId) {

        meterRegistry
                .counter(
                        "ehconsumer_events_received",
                        Tags.of("partitionId", partitionId)
                ).increment();
    }

    public void countPartitionInitialization(final String partitionId) {

        meterRegistry
                .counter(
                        "ehconsumer_partition_initialization",
                        Tags.of("partitionId", partitionId)
                ).increment();
    }

    public void countProcessError(final String partitionId) {

        meterRegistry
                .counter(
                        "ehconsumer_process_errors",
                        Tags.of("partitionId", partitionId)
                ).increment();
    }

    public void countPartitionClose(final String partitionId) {

        meterRegistry
                .counter(
                        "ehconsumer_partition_close",
                        Tags.of("partitionId", partitionId)
                ).increment();
    }

    public void updateLag(final String partitionId, final long lag) {

        if (lags.containsKey(partitionId)) {
            lags.get(partitionId).set(lag);
        } else {
            lags.put(
                    partitionId,
                    meterRegistry.gauge(
                            "ehconsumer_lag",
                            Tags.of("partitionId", partitionId),
                            new AtomicLong(lag)
                    )
            );
        }
    }

    public void updateEventTimestamp(final String partitionId, final long timestamp) {

        if (eventTimestamp.containsKey(partitionId)) {
            eventTimestamp.get(partitionId).set(timestamp);
        } else {
            eventTimestamp.put(
                    partitionId,
                    meterRegistry.gauge(
                            "ehconsumer_event_timestamp",
                            Tags.of("partitionId", partitionId),
                            new AtomicLong(timestamp)
                    )
            );
        }
    }
}
