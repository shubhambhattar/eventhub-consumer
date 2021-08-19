package com.example.demo.impl;

import com.azure.messaging.eventhubs.models.EventContext;
import com.example.demo.metrics.ConsumerMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Slf4j
@Service
public class ProcessEvent implements Consumer<EventContext> {

    private final ConsumerMetrics consumerMetrics;
    private final int CHECKPOINT_AFTER_EVENTS;

    @Autowired
    public ProcessEvent(final ConsumerMetrics consumerMetrics,
                        @Value("${in-memory.checkpoint-after-events}") final int checkpointAfterEvents) {
        this.consumerMetrics = consumerMetrics;
        this.CHECKPOINT_AFTER_EVENTS = checkpointAfterEvents;
    }

    @Override
    public void accept(EventContext eventContext) {

        final String partitionId = eventContext.getPartitionContext().getPartitionId();
        try {
            final long lag = eventContext.getLastEnqueuedEventProperties().getSequenceNumber()
                    - eventContext.getEventData().getSequenceNumber();

            consumerMetrics.countEvents(partitionId);
            consumerMetrics.updateLag(partitionId, lag);
            consumerMetrics.updateEventTimestamp(partitionId, eventContext.getEventData().getEnqueuedTime().toEpochMilli());

            if (eventContext.getEventData().getSequenceNumber() % CHECKPOINT_AFTER_EVENTS == 0) {
                consumerMetrics.getCheckpointTimer().record(eventContext::updateCheckpoint);
            }
        } catch (Exception e) {
            consumerMetrics.countUnknownErrorWhileProcessing(partitionId);
            log.error("Unknown error while counting click: ", e);
        }
    }
}
