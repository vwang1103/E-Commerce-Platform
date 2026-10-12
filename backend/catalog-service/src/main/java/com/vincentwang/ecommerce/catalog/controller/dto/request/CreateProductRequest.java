package com.vincentwang.ecommerce.catalog.controller.dto.request;

import com.vincentwang.ecommerce.catalog.model.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotNull @Positive Long categoryId,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 5000) String description,
        @NotNull @PositiveOrZero Long priceMinor,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull ProductStatus status) {}
