package com.example.demo.impl;

import com.azure.messaging.eventhubs.models.CloseContext;
import com.example.demo.metrics.ConsumerMetrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Slf4j
@Service
@AllArgsConstructor
public class PartitionClose implements Consumer<CloseContext> {

    private final ConsumerMetrics consumerMetrics;

    @Override
    public void accept(CloseContext closeContext) {

        final String partitionId = closeContext.getPartitionContext().getPartitionId();
        consumerMetrics.countPartitionClose(partitionId);

        log.info("Partition: {} closed for reason: {}", partitionId, closeContext.getCloseReason());
    }
}
