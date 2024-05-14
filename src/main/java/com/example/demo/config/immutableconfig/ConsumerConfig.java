package com.example.demo.config.immutableconfig;

import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.models.EventPosition;
import com.example.demo.impl.PartitionClose;
import com.example.demo.impl.ProcessError;
import com.example.demo.impl.ProcessEvent;
import com.example.demo.impl.ProcessPartitionInitialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ConfigurationProperties(prefix = "consumer")
public record ConsumerConfig(String connectionString,
                             String consumerGroup,
                             String initialPartitionEventPosition,
                             int noOfPartitions,
                             boolean trackLastEnqueuedEventProperties,
                             String fullyQualifiedNamespace,
                             String eventHubName) {

    @Bean
    public Map<String, EventPosition> getInitialPartitionEventPosition() {

        Map<String, EventPosition> initialPartitionEventPosition = new HashMap<>();

        log.info("--- EventHub Consumer Configuration ---");
        log.info(toString());
        log.info("---------------------------------------");

        switch (initialPartitionEventPosition()) {
            case "earliest" -> {
                for (int i = 0; i < noOfPartitions; i++) {
                    initialPartitionEventPosition.put(String.valueOf(i), EventPosition.earliest());
                }
            }
            case "latest" -> {
                for (int i = 0; i < noOfPartitions; i++) {
                    initialPartitionEventPosition.put(String.valueOf(i), EventPosition.latest());
                }
            }
            default -> {
                Instant enqueuedTime = Instant.parse(initialPartitionEventPosition());
                for (int i = 0; i < noOfPartitions; i++) {
                    initialPartitionEventPosition.put(String.valueOf(i), EventPosition.fromEnqueuedTime(enqueuedTime));
                }
            }
        }

        return initialPartitionEventPosition;
    }

    @Bean(destroyMethod = "stop")
    public EventProcessorClient getEventProcessorClient(final Map<String, EventPosition> initialPartitionEventPosition,
                                                        final ProcessPartitionInitialization processPartitionInitialization,
                                                        final ProcessEvent processEvent,
                                                        final ProcessError processError,
                                                        final PartitionClose partitionClose,
                                                        final CheckpointStore checkpointStore) {

        log.info("--- EventHub Consumer Configuration ---");
        log.info(toString());
        log.info("---------------------------------------");

        return new EventProcessorClientBuilder()
                .connectionString(connectionString)
                .consumerGroup(consumerGroup)
                .trackLastEnqueuedEventProperties(trackLastEnqueuedEventProperties)
                .initialPartitionEventPosition(initialPartitionEventPosition)
                .processPartitionInitialization(processPartitionInitialization)
                .checkpointStore(checkpointStore)
                .processEvent(processEvent)
                .processError(processError)
                .processPartitionClose(partitionClose)
                .buildEventProcessorClient();
    }

    @Override
    public String toString() {
        return "ConsumerConfig{" +
                "consumerGroup='" + consumerGroup + '\'' +
                ", initialPartitionEventPosition='" + initialPartitionEventPosition + '\'' +
                ", noOfPartitions=" + noOfPartitions +
                ", trackLastEnqueuedEventProperties=" + trackLastEnqueuedEventProperties +
                ", fullyQualifiedNamespace='" + fullyQualifiedNamespace + '\'' +
                ", eventHubName='" + eventHubName + '\'' +
                '}';
    }
}
