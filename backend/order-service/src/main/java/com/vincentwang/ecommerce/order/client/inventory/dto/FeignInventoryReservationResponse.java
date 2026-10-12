package com.vincentwang.ecommerce.order.client.inventory.dto;

import java.time.Instant;

public record FeignInventoryReservationResponse(
        long orderId, String userId, long productId, long quantity, String status, Instant expiresAt) {}
