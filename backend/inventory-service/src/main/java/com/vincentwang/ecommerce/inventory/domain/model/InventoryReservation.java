package com.vincentwang.ecommerce.inventory.domain.model;

import java.time.Instant;

public record InventoryReservation(
        long orderId,
        String userId,
        long productId,
        long quantity,
        ReservationStatus status,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt) {}
