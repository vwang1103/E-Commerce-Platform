package com.vincentwang.ecommerce.inventory.controller.dto.response;

import com.vincentwang.ecommerce.inventory.model.InventoryReservation;
import com.vincentwang.ecommerce.inventory.model.ReservationStatus;
import java.time.Instant;

public record ReservationResponse(
        long orderId, String userId, long productId, long quantity, ReservationStatus status, Instant expiresAt) {

    public static ReservationResponse from(InventoryReservation reservation) {
        return new ReservationResponse(
                reservation.orderId(),
                reservation.userId(),
                reservation.productId(),
                reservation.quantity(),
                reservation.status(),
                reservation.expiresAt());
    }
}
