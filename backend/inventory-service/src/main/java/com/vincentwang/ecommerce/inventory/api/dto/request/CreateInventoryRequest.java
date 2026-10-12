package com.vincentwang.ecommerce.inventory.api.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record CreateInventoryRequest(@PositiveOrZero long totalQuantity) {}
