package com.vincentwang.ecommerce.order.client.catalog.dto;

public record FeignProductResponse(
        long productId, String userId, String name, long priceMinor, String currency, String status) {}
