package com.vincentwang.ecommerce.order.infrastructure.client.dto;

import java.time.Instant;

public record FeignInventoryReservationResponse(
        long orderId, String userId, long productId, long quantity, String status, Instant expiresAt) {}
