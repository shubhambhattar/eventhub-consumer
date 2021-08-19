package com.example.demo.service;

import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.models.PartitionOwnership;
import com.example.demo.config.immutableconfig.ConsumerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EventProcessorClientHealthIndicator implements HealthIndicator {

    private final EventProcessorClient eventProcessorClient;
    private final CheckpointStore checkpointStore;
    private final ConsumerConfig consumerConfig;
    private final Duration maxDuration;

    public EventProcessorClientHealthIndicator(final EventProcessorClient eventProcessorClient,
                                               final CheckpointStore checkpointStore,
                                               final ConsumerConfig consumerConfig,
                                               @Value("${eventhub-partition.max-duration-of-inactivity}") final Duration maxDuration) {

        this.eventProcessorClient = eventProcessorClient;
        this.checkpointStore = checkpointStore;
        this.consumerConfig = consumerConfig;
        this.maxDuration = maxDuration;

    }

    private List<PartitionOwnership> listPartitionOwnerships() {

        final List<PartitionOwnership> partitionOwnerships = checkpointStore
                .listOwnership(
                        consumerConfig.getFullyQualifiedNamespace(),
                        consumerConfig.getEventHubName(),
                        consumerConfig.getConsumerGroup()
                )
                .toStream()
                .filter(partitionOwnership ->
                        partitionOwnership.getOwnerId().compareTo(eventProcessorClient.getIdentifier()) == 0)
                .collect(Collectors.toList());

        if (log.isInfoEnabled()) {
            log.info("--- Partition Ownership information ---");
            log.info("Size: {}", partitionOwnerships.size());
            for (PartitionOwnership partitionOwnership: partitionOwnerships) {
                log.info("Partition ID: {}, Owner ID: {}, Last Modified Time: {}",
                        partitionOwnership.getPartitionId(),
                        partitionOwnership.getOwnerId(),
                        partitionOwnership.getLastModifiedTime()
                );
            };
            log.info("--- Partition Ownership information end ---");
        }
        return partitionOwnerships;
    }

    @Override
    public Health health() {

        if (!eventProcessorClient.isRunning()) {
            return Health.down().withDetail("EventProcessorClient is not running", "").build();
        }

        final Instant currentInstant = Instant.now();
        final List<PartitionOwnership> partitionOwnerships = listPartitionOwnerships();

        for (PartitionOwnership partitionOwnership: partitionOwnerships) {

            final Instant lastModifiedTime = Instant.ofEpochMilli(partitionOwnership.getLastModifiedTime());
            if (Duration
                    .between(currentInstant, lastModifiedTime)
                    .compareTo(maxDuration) > 0) {

                log.error(
                        "Last event processed in partition: {} was {} seconds ago. Marking health as down.",
                        partitionOwnership.getPartitionId(),
                        Duration.between(currentInstant, lastModifiedTime).getSeconds()
                );

                return Health
                        .down()
                        .withDetail(
                                String.format(
                                        "Events not consumed on given partition since last %s seconds",
                                        maxDuration.getSeconds()
                                ),
                                partitionOwnership.getPartitionId()
                        )
                        .build();
            }
        }
        return Health.up().build();
    }
}
