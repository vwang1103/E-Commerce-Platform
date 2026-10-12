package com.vincentwang.ecommerce.catalog.infrastructure.messaging;

import com.vincentwang.ecommerce.common.messaging.IOutboxRecord;
import java.time.LocalDateTime;

public record CatalogOutboxEvent(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        LocalDateTime occurredAt)
        implements IOutboxRecord {}
