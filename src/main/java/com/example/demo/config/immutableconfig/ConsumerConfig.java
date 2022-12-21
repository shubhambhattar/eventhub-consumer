package com.example.demo.config.immutableconfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ToString(exclude = "connectionString")
@ConfigurationProperties(prefix = "consumer")
public class ConsumerConfig {

    private final String connectionString;
    private final String consumerGroup;
    private final String initialPartitionEventPosition;
    private final int noOfPartitions;
    private final boolean trackLastEnqueuedEventProperties;
    private final String fullyQualifiedNamespace;
    private final String eventHubName;
}
