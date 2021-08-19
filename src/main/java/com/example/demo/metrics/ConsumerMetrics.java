package com.example.demo.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class ConsumerMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicLong> lags = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventTimestamp = new ConcurrentHashMap<>();
    @Getter private final Timer checkpointTimer;

    public ConsumerMetrics(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.checkpointTimer = Timer
                .builder("ehconsumer_checkpoint")
                .publishPercentiles(0.75, 0.90, 0.95, 0.98, 0.99, 0.999)
                .description("Time taken to checkpoint")
                .register(this.meterRegistry);
    }

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

    public void countUnknownErrorWhileProcessing(final String partitionId) {

        meterRegistry
                .counter(
                        "eh_unknown_exception",
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