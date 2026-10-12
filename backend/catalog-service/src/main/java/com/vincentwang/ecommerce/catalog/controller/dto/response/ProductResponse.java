package com.vincentwang.ecommerce.catalog.controller.dto.response;

import com.vincentwang.ecommerce.catalog.model.Product;
import com.vincentwang.ecommerce.catalog.model.ProductStatus;
import java.time.LocalDateTime;

public record ProductResponse(
        Long productId,
        String userId,
        Long categoryId,
        String name,
        String description,
        Long priceMinor,
        String currency,
        ProductStatus status,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getUserId(),
                product.getCategoryId(),
                product.getName(),
                product.getDescription(),
                product.getPriceMinor(),
                product.getCurrency(),
                product.getStatus(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
