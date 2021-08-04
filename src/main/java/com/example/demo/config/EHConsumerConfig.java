package com.example.demo.config;

import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.models.EventPosition;
import com.example.demo.config.immutableconfig.ConsumerConfig;
import com.example.demo.impl.PartitionClose;
import com.example.demo.impl.ProcessError;
import com.example.demo.impl.ProcessEvent;
import com.example.demo.impl.ProcessPartitionInitialization;
import com.example.demo.metrics.ConsumerMetrics;
import com.example.demo.service.LastEventProcessedTrackingService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class EHConsumerConfig {

    @Bean
    public PartitionClose getPartitionClose(final ConsumerMetrics consumerMetrics, 
                                            final LastEventProcessedTrackingService lastEventProcessedTrackingService) {
        return new PartitionClose(consumerMetrics, lastEventProcessedTrackingService);
    }

    @Bean
    public ProcessError getProcessError(final ConsumerMetrics consumerMetrics) {
        return new ProcessError(consumerMetrics);
    }

    @Bean
    public ProcessEvent getProcessEvent(final ConsumerMetrics consumerMetrics,
                                        final LastEventProcessedTrackingService lastEventProcessedTrackingService) {
        return new ProcessEvent(consumerMetrics, lastEventProcessedTrackingService);
    }

    @Bean
    public ProcessPartitionInitialization getPartitionInitialization(final ConsumerMetrics consumerMetrics) {
        return new ProcessPartitionInitialization(consumerMetrics);
    }

    @Bean
    public Map<String, EventPosition> getInitialPartitionEventPosition(final ConsumerConfig consumerConfig) {

        Map<String, EventPosition> initialPartitionEventPosition = new HashMap<>();

        log.info("--- EventHub Consumer Configuration ---");
        log.info(consumerConfig.toString());
        log.info("---------------------------------------");

        switch (consumerConfig.getInitialPartitionEventPosition()) {

            case "earliest": {

                for (int i = 0; i < consumerConfig.getNoOfPartitions(); i++) {
                    initialPartitionEventPosition.put(String.valueOf(i), EventPosition.earliest());
                }
                break;
            }

            case "latest": {

                for (int i = 0; i < consumerConfig.getNoOfPartitions(); i++) {
                    initialPartitionEventPosition.put(String.valueOf(i), EventPosition.latest());
                }
                break;
            }

            default: {

                Instant enqueuedTime = Instant.parse(consumerConfig.getInitialPartitionEventPosition());

                for (int i = 0; i < consumerConfig.getNoOfPartitions(); i++) {
                    initialPartitionEventPosition.put(String.valueOf(i), EventPosition.fromEnqueuedTime(enqueuedTime));
                }
                break;
            }
        }

        return initialPartitionEventPosition;
    }

    @Bean(destroyMethod = "stop")
    public EventProcessorClient getEventProcessorClient(final ConsumerConfig consumerConfig,
                                                        final Map<String, EventPosition> initialPartitionEventPosition,
                                                        final ProcessPartitionInitialization processPartitionInitialization,
                                                        final ProcessEvent processEvent,
                                                        final ProcessError processError,
                                                        final PartitionClose partitionClose,
                                                        final CheckpointStore checkpointStore) {

        log.info("--- EventHub Consumer Configuration ---");
        log.info(consumerConfig.toString());
        log.info("---------------------------------------");

        return new EventProcessorClientBuilder()
                .connectionString(consumerConfig.getConnectionString())
                .consumerGroup(consumerConfig.getConsumerGroup())
                .trackLastEnqueuedEventProperties(consumerConfig.isTrackLastEnqueuedEventProperties())
                .initialPartitionEventPosition(initialPartitionEventPosition)
                .processPartitionInitialization(processPartitionInitialization)
                .checkpointStore(checkpointStore)
                .processEvent(processEvent)
                .processError(processError)
                .processPartitionClose(partitionClose)
                .buildEventProcessorClient();
    }
}
