package com.vincentwang.ecommerce.order.infrastructure.messaging;

import com.vincentwang.ecommerce.common.messaging.IOutboxRecord;
import java.time.LocalDateTime;

/** One row of {@code order_outbox}. The user ID is the shard key, so every lookup carries it. */
public record OrderOutboxEvent(
        String userId,
        String eventId,
        String aggregateId,
        String eventType,
        String partitionKey,
        String payload,
        LocalDateTime occurredAt)
        implements IOutboxRecord {

    @Override
    public String aggregateType() {
        return "order";
    }
}
