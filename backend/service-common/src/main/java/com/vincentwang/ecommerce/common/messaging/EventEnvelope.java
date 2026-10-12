package com.vincentwang.ecommerce.common.messaging;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record EventEnvelope(
        String eventId,
        String eventType,
        Instant occurredAt,
        String traceId,
        String aggregateType,
        String aggregateId,
        int schemaVersion,
        JsonNode payload) {}
