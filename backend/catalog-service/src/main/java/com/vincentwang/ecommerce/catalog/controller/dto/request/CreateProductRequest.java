package com.vincentwang.ecommerce.catalog.controller.dto.request;

import com.vincentwang.ecommerce.catalog.model.ProductStatus;
import jakarta.validation.constraints.*;

public record CreateProductRequest(
        @NotNull @Positive Long categoryId,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 5000) String description,
        @NotNull @PositiveOrZero Long priceMinor,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull ProductStatus status) {}
