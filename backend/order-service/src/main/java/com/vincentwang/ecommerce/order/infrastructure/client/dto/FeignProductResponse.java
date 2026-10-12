package com.vincentwang.ecommerce.order.infrastructure.client.dto;

public record FeignProductResponse(
        long productId, String userId, String name, long priceMinor, String currency, String status) {}
