package com.vincentwang.ecommerce.order.application.model;

/** The catalog's current name, price, and availability for a product. */
public record ProductQuote(
        long productId, String userId, String name, long unitPriceMinor, String currency, boolean purchasable) {}
