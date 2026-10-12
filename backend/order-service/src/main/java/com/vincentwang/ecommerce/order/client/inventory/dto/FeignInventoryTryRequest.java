package com.vincentwang.ecommerce.order.client.inventory.dto;

import java.util.List;

public record FeignInventoryTryRequest(
        long orderId, String userId, List<FeignInventoryReservationItem> items, long ttlSeconds) {}
