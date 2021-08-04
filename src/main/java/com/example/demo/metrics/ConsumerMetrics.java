package com.example.demo.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@AllArgsConstructor
public class ConsumerMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicLong> lags = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventTimestamp = new ConcurrentHashMap<>();

    public void countEvents(final String partitionId) {

        meterRegistry
                .counter(
                        "ehconsumer_events_received",
                        Tags.of("partition_id", partitionId)
                ).increment();
    }

    public void countCheckpoint(final String partitionId) {

        meterRegistry
                .counter(
                        "ehconsumer_checkpoint",
                        Tags.of("partition_id", partitionId)
                ).increment();
    }

    public void countPartitionInitialization(final String partitionId) {

        meterRegistry
                .counter(
                        "ehconsumer_partition_initialization",
                        Tags.of("partition_id", partitionId)
                ).increment();
    }

    public void countProcessError(final String partitionId) {

        meterRegistry
                .counter(
                        "ehconsumer_process_errors",
                        Tags.of("partition_id", partitionId)
                ).increment();
    }

    public void countPartitionClose(final String partitionId) {

        meterRegistry
                .counter(
                        "ehconsumer_partition_close",
                        Tags.of("partition_id", partitionId)
                ).increment();
    }

    public void updateLag(final String partitionId, final long lag) {

        lags.compute(partitionId, (k, v) -> {
            if(v == null) {
                return meterRegistry.gauge(
                        "ehconsumer_lag",
                        Tags.of("partition_id", partitionId),
                        new AtomicLong(lag)
                );
            }
            v.set(lag);
            return v;
        });
    }

    public void updateEventTimestamp(final String partitionId, final long timestamp) {

        eventTimestamp.compute(partitionId, (k, v) -> {
            if(v == null) {
                return meterRegistry.gauge(
                        "ehconsumer_event_timestamp",
                        Tags.of("partition_id", partitionId),
                        new AtomicLong(timestamp)
                );
            }
            v.set(timestamp);
            return v;
        });
    }
}