package com.vincentwang.ecommerce.catalog.model;

/** Product data stored in and returned by the search index. */
public record ProductSearchDocument(
        long productId,
        String userId,
        long categoryId,
        String name,
        String description,
        long priceMinor,
        String currency,
        ProductStatus status,
        long version) {}
