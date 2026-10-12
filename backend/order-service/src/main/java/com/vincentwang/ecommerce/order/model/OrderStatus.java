package com.vincentwang.ecommerce.order.model;

public enum OrderStatus {
    ACCEPTED,
    PENDING_PAYMENT,
    CONFIRMING_INVENTORY,
    INVENTORY_CONFIRMATION_FAILED,
    PAID,
    EXPIRED,
    REJECTED,
    FAILED
}
