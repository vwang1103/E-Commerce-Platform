package com.vincentwang.ecommerce.catalog.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private Long productId;
    private String userId;
    private Long categoryId;
    private String name;
    private String description;
    private Long priceMinor;
    private String currency;
    private ProductStatus status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
