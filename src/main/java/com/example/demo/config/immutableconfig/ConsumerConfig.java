package com.example.demo.config.immutableconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer")
public record ConsumerConfig(String connectionString,
                             String consumerGroup,
                             String initialPartitionEventPosition,
                             int noOfPartitions,
                             boolean trackLastEnqueuedEventProperties,
                             String fullyQualifiedNamespace,
                             String eventHubName) {

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
