package com.example.demo.service;

import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.models.Checkpoint;
import com.azure.messaging.eventhubs.models.PartitionOwnership;
import com.example.demo.config.immutableconfig.ConsumerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EventProcessorClientHealthIndicator implements HealthIndicator {

    private final EventProcessorClient eventProcessorClient;
    private final CheckpointStore checkpointStore;
    private final ConsumerConfig consumerConfig;
    private Map<String, Checkpoint> oldMappedCheckpoints;

    public EventProcessorClientHealthIndicator(final EventProcessorClient eventProcessorClient,
                                               final CheckpointStore checkpointStore,
                                               final ConsumerConfig consumerConfig) {

        this.eventProcessorClient = eventProcessorClient;
        this.checkpointStore = checkpointStore;
        this.consumerConfig = consumerConfig;
        oldMappedCheckpoints = new HashMap<>();
    }

    /**
     * Returns all the partitions claimed by the current instance of the application.
     * @return set containing all the partition ids that are claimed by current instance of the application.
     */
    private Set<String> getClaimedPartitions() {

        final Set<String> claimedPartitions = checkpointStore
                .listOwnership(
                        consumerConfig.fullyQualifiedNamespace(),
                        consumerConfig.eventHubName(),
                        consumerConfig.consumerGroup()
                )
                .toStream()
                .filter(partitionOwnership ->
                        partitionOwnership.getOwnerId().compareTo(eventProcessorClient.getIdentifier()) == 0)
                .map(PartitionOwnership::getPartitionId)
                .collect(Collectors.toSet());

        if (log.isInfoEnabled()) {
            log.info("--- Partition Ownership information ---");
            log.info("Size: {}", claimedPartitions.size());
            for (String partitionId: claimedPartitions) {
                log.info("Partition ID: {}, Owner ID: {}",
                        partitionId,
                        eventProcessorClient.getIdentifier()
                );
            };
            log.info("--- Partition Ownership information end ---");
        }
        return claimedPartitions;
    }

    /**
     * Returns the checkpoint data for all the partitions that are claimed by the current instance of the application.
     * @param claimedPartitions set containing all the partitions ids that are claimed.
     * @return map that maps the checkpoint information for all the claimed partitions
     */
    private Map<String, Checkpoint> getCheckpointDataForClaimedPartitions(final Set<String> claimedPartitions) {

        final Map<String, Checkpoint> checkpoints = checkpointStore
                .listCheckpoints(
                        consumerConfig.fullyQualifiedNamespace(),
                        consumerConfig.eventHubName(),
                        consumerConfig.consumerGroup()
                )
                .toStream()
                .filter(checkpoint -> claimedPartitions.contains(checkpoint.getPartitionId()))
                .collect(Collectors.toMap(Checkpoint::getPartitionId, checkpoint -> checkpoint));

        if (log.isInfoEnabled()) {
            log.info("--- Checkpoint information ---");
            log.info("Size: {}", checkpoints.size());
            for (Map.Entry<String, Checkpoint> entry: checkpoints.entrySet()) {
                log.info("Partition ID: {}, Offset: {}, Sequence: {}",
                        entry.getKey(),
                        entry.getValue().getOffset(),
                        entry.getValue().getSequenceNumber()
                );
            };
            log.info("--- Checkpoint information end ---");
        }
        return checkpoints;
    }

    @Override
    public Health health() {

        if (!eventProcessorClient.isRunning()) {
            return Health.down().withDetail("EventProcessorClient is not running", "").build();
        }

        final Set<String> claimedPartitions = getClaimedPartitions();
        final Map<String, Checkpoint> currentMappedCheckpoints = getCheckpointDataForClaimedPartitions(claimedPartitions);

        // If starting for the first time, this will be empty
        if (oldMappedCheckpoints.size() == 0) {
            oldMappedCheckpoints = currentMappedCheckpoints;
            return Health.up().build();
        }

        for (Map.Entry<String, Checkpoint> entry: currentMappedCheckpoints.entrySet()) {

            final String partitionId = entry.getKey();
            if (oldMappedCheckpoints.containsKey(partitionId) &&
                    oldMappedCheckpoints.get(partitionId).getSequenceNumber().longValue()
                            == entry.getValue().getSequenceNumber().longValue()) {

                log.error("Events not read from partition: {}. Marking health as down.", partitionId);
                return Health
                        .down()
                        .withDetail("Not consuming events from partition", entry.getKey())
                        .build();
            }
        }
        oldMappedCheckpoints = currentMappedCheckpoints;
        return Health.up().build();
    }
}
