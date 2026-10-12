package com.vincentwang.ecommerce.common.messaging;

import java.time.ZoneOffset;
import java.util.List;

/**
 * The publishing half of the transactional outbox pattern, shared by every service that emits events.
 *
 * <p>A subclass says where rows are stored and where each event goes; this class owns the order of
 * operations: claim rows, publish and wait for the broker acknowledgement, then mark the row published.
 * The caller must run {@link #relayBatch()} inside one database transaction that holds the row locks, so a
 * crash before the mark produces a duplicate delivery, never a lost event. Consumers deduplicate by event ID.
 */
public abstract class AOutboxRelay<E extends IOutboxRecord> {

    private static final int SCHEMA_VERSION = 1;

    private final KafkaEventPublisher publisher;
    private final EventJsonMapper jsonMapper;
    private final String sourceName;

    protected AOutboxRelay(KafkaEventPublisher publisher, EventJsonMapper jsonMapper, String sourceName) {
        this.publisher = publisher;
        this.jsonMapper = jsonMapper;
        this.sourceName = sourceName;
    }

    /** Returns the oldest unpublished rows, locked for this transaction (for example FOR UPDATE SKIP LOCKED). */
    protected abstract List<E> claimBatch();

    protected abstract String topicFor(E event);

    /** The Kafka key; events with the same key stay ordered. */
    protected abstract String keyFor(E event);

    protected abstract void markPublished(E event);

    protected final int relayBatch() throws Exception {
        int published = 0;
        for (E event : claimBatch()) {
            publisher.publish(topicFor(event), keyFor(event), toEnvelope(event));
            markPublished(event);
            published++;
        }
        return published;
    }

    private EventEnvelope toEnvelope(E event) {
        return new EventEnvelope(
                event.eventId(),
                event.eventType(),
                event.occurredAt().toInstant(ZoneOffset.UTC),
                event.eventId().replace("-", ""),
                event.aggregateType(),
                event.aggregateId(),
                SCHEMA_VERSION,
                jsonMapper.parsePayload(event.payload(), sourceName + " outbox contains invalid JSON"));
    }
}
