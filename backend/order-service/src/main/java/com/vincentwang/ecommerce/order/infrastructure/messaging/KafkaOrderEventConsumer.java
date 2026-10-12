package com.vincentwang.ecommerce.order.infrastructure.messaging;

import com.vincentwang.ecommerce.common.messaging.EventEnvelope;
import com.vincentwang.ecommerce.common.messaging.EventJsonMapper;
import com.vincentwang.ecommerce.order.application.IOrderService;
import com.vincentwang.ecommerce.order.application.event.OrderEvents;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Turns broker messages into order use cases; deduplication and state rules live in {@link IOrderService}. */
@Component
@ConditionalOnProperty(prefix = "order.events", name = "enabled", havingValue = "true")
public class KafkaOrderEventConsumer {
    private static final String INVALID_EVENT = "Invalid order event";

    private final IOrderService orderService;
    private final EventJsonMapper eventJsonMapper;

    public KafkaOrderEventConsumer(IOrderService orderService, EventJsonMapper eventJsonMapper) {
        this.orderService = orderService;
        this.eventJsonMapper = eventJsonMapper;
    }

    @KafkaListener(topics = "${order.events.workflow-topic}", groupId = "${order.events.workflow-consumer-group}")
    public void consumeWorkflow(String message) {
        EventEnvelope event = eventJsonMapper.deserializeEnvelope(message, INVALID_EVENT);
        if (!OrderEvents.ORDER_REQUESTED.equals(event.eventType())) {
            throw new IllegalArgumentException("Unsupported workflow event: " + event.eventType());
        }
        OrderEvents.OrderRequested payload =
                eventJsonMapper.convertPayload(event.payload(), OrderEvents.OrderRequested.class, INVALID_EVENT);
        orderService.activate(event.eventId(), payload.userId(), payload.orderId());
    }

    @KafkaListener(topics = "${order.events.inventory-topic}", groupId = "${order.events.inventory-consumer-group}")
    public void consumeInventory(String message) {
        EventEnvelope event = eventJsonMapper.deserializeEnvelope(message, INVALID_EVENT);
        OrderEvents.InventoryCommand payload =
                eventJsonMapper.convertPayload(event.payload(), OrderEvents.InventoryCommand.class, INVALID_EVENT);
        orderService.applyInventoryCommand(event.eventId(), event.eventType(), payload.userId(), payload.orderId());
    }

    /** Records terminal confirm failure after the shared Kafka error handler exhausts its retries. */
    @KafkaListener(
            topics = "${order.events.inventory-topic}.DLT",
            groupId = "${order.events.inventory-consumer-group}-failure-state")
    public void consumeInventoryDeadLetter(String message) {
        EventEnvelope event = eventJsonMapper.deserializeEnvelope(message, INVALID_EVENT);
        if (!OrderEvents.INVENTORY_CONFIRM.equals(event.eventType())) {
            return;
        }
        OrderEvents.InventoryCommand payload =
                eventJsonMapper.convertPayload(event.payload(), OrderEvents.InventoryCommand.class, INVALID_EVENT);
        orderService.markInventoryConfirmationFailed(payload.userId(), payload.orderId());
    }
}
