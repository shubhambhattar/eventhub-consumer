package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class OldestEnqueuedEventProcessedTrackingService implements HealthIndicator {

    private final Map<String, AtomicLong> lastEventProcessedTimestampPerPartition = new ConcurrentHashMap<>();

    public void update(final String partitionId, final long timestamp) {
        lastEventProcessedTimestampPerPartition.compute(partitionId, (k, v) -> {
            if (v == null) {
                return new AtomicLong(timestamp);
            }
            v.set(timestamp);
            return v;
        });
    }

    public Optional<AtomicLong> get(final String partitionId) {
        return Optional.ofNullable(lastEventProcessedTimestampPerPartition.get(partitionId));
    }

    public void remove(final String partitionId) {
        lastEventProcessedTimestampPerPartition.remove(partitionId);
    }

    private long getOldestEnqueuedEventProcessedInAnyPartitionTimestamp() {

        long oldestTimestampEncountered = Instant.now().toEpochMilli();
        for (String partitionId: lastEventProcessedTimestampPerPartition.keySet()) {
            final long lastEventProcessedTimestamp = lastEventProcessedTimestampPerPartition.get(partitionId).get();
            if (lastEventProcessedTimestamp < oldestTimestampEncountered) {
                oldestTimestampEncountered = lastEventProcessedTimestamp;
            }
        }
        return oldestTimestampEncountered;
    }

    @Override
    public Health health() {
        final long currentTimestamp = Instant.now().toEpochMilli();

        if (currentTimestamp - getOldestEnqueuedEventProcessedInAnyPartitionTimestamp() > 300_000) {
            return Health.down()
                    .withDetail("Events not consumed one partition since last 5 mins", "")
                    .build();
        }
        return Health.up().build();
    }
}
