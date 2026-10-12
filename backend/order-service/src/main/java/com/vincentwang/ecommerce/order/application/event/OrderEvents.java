package com.vincentwang.ecommerce.order.application.event;

import java.util.List;

/**
 * Every event the order service emits and consumes, in one place.
 *
 * <p>Order flow: {@code order.requested} activates the order for payment; payment or expiry then writes an
 * inventory command ({@code inventory.confirm} or {@code inventory.cancel}) that is applied to the inventory
 * service asynchronously. Commands and workflow events are keyed by order ID so a multi-item order remains one
 * ordered event stream.
 */
public final class OrderEvents {

    public static final String ORDER_REQUESTED = "order.requested";
    public static final String INVENTORY_CONFIRM = "inventory.confirm";
    public static final String INVENTORY_CANCEL = "inventory.cancel";

    private OrderEvents() {}

    public static boolean isInventoryCommand(String eventType) {
        return INVENTORY_CONFIRM.equals(eventType) || INVENTORY_CANCEL.equals(eventType);
    }

    /** Payload of {@link #ORDER_REQUESTED}. */
    public record OrderRequested(long orderId, String userId, List<Long> productIds) {}

    /** Payload of {@link #INVENTORY_CONFIRM} and {@link #INVENTORY_CANCEL}; {@code reason} is a trace aid. */
    public record InventoryCommand(long orderId, String userId, String reason) {}
}
