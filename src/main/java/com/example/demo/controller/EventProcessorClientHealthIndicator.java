package com.example.demo.controller;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.example.demo.service.LastEventProcessedTrackingService;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@AllArgsConstructor
public class EventProcessorClientHealthIndicator implements HealthIndicator {

    private final EventProcessorClient eventProcessorClient;
    private final LastEventProcessedTrackingService lastEventProcessedTrackingService;

    @Override
    public Health health() {

        final long currentTimestamp = Instant.now().toEpochMilli();

        if (currentTimestamp - lastEventProcessedTrackingService.getOldestEventProcessedInAnyPartitionTimestamp() > 300_000) {
            return Health.down()
                    .withDetail("Events not consumed one partition since last 5 mins", "")
                    .build();
        }

        if (eventProcessorClient.isRunning()) {
            return Health.up().build();
        }
        return Health.down().withDetail("EventProcessorClient is not running", "").build();
    }
}
