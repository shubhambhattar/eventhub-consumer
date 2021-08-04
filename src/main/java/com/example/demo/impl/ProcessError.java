package com.example.demo.impl;

import com.azure.messaging.eventhubs.models.ErrorContext;
import com.example.demo.metrics.ConsumerMetrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Slf4j
@Service
@AllArgsConstructor
public class ProcessError implements Consumer<ErrorContext> {

    private final ConsumerMetrics consumerMetrics;

    @Override
    public void accept(ErrorContext errorContext) {

        final String partitionId = errorContext.getPartitionContext().getPartitionId();
        consumerMetrics.countProcessError(partitionId);

        log.error(
                "Error while consuming from EventHub partition {}",
                partitionId,
                errorContext.getThrowable()
        );
    }
}
