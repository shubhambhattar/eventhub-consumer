package com.example.demo.impl;

import com.azure.messaging.eventhubs.models.EventContext;
import com.example.demo.metrics.ConsumerMetrics;
import com.example.demo.service.LastEventProcessedTrackingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Slf4j
@Service
@AllArgsConstructor
public class ProcessEvent implements Consumer<EventContext> {

    private final ConsumerMetrics consumerMetrics;
    private final LastEventProcessedTrackingService lastEventProcessedTrackingService;

    @Override
    public void accept(EventContext eventContext) {

        final String partitionId = eventContext.getPartitionContext().getPartitionId();
        final long lag = eventContext.getLastEnqueuedEventProperties().getSequenceNumber()
                - eventContext.getEventData().getSequenceNumber();

        consumerMetrics.countEvents(partitionId);
        consumerMetrics.updateLag(partitionId, lag);
        lastEventProcessedTrackingService
                .update(partitionId, eventContext.getEventData().getEnqueuedTime().toEpochMilli());
        consumerMetrics.updateEventTimestamp(partitionId, eventContext.getEventData().getEnqueuedTime().toEpochMilli());
    }
}
