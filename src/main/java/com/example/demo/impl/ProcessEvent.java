package com.example.demo.impl;

import com.azure.messaging.eventhubs.models.EventContext;
import com.example.demo.metrics.ConsumerMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Slf4j
@Service
public class ProcessEvent implements Consumer<EventContext> {

    private final ConsumerMetrics consumerMetrics;

    public ProcessEvent(final ConsumerMetrics consumerMetrics) {
        this.consumerMetrics = consumerMetrics;
    }

    @Override
    public void accept(EventContext eventContext) {

        final String partitionId = eventContext.getPartitionContext().getPartitionId();
        final long lag = eventContext.getLastEnqueuedEventProperties().getSequenceNumber()
                - eventContext.getEventData().getSequenceNumber();

        consumerMetrics.countEvents(partitionId);
        consumerMetrics.updateLag(partitionId, lag);
        consumerMetrics.updateEventTimestamp(partitionId, eventContext.getEventData().getEnqueuedTime().toEpochMilli());
    }
}
