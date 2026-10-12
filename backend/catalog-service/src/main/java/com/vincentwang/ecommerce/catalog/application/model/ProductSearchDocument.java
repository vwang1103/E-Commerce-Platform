package com.vincentwang.ecommerce.catalog.application.model;

import com.vincentwang.ecommerce.catalog.domain.model.ProductStatus;

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
