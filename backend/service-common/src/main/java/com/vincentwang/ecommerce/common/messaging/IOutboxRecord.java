package com.vincentwang.ecommerce.common.messaging;

import java.time.LocalDateTime;

/** A row of a service's transactional outbox, as read by the relay that forwards it to Kafka. */
public interface IOutboxRecord {

    String eventId();

    String eventType();

    String aggregateType();

    String aggregateId();

    /** The JSON payload exactly as stored. */
    String payload();

    /** Database time in UTC. */
    LocalDateTime occurredAt();
}
