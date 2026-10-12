package com.vincentwang.ecommerce.inventory.controller.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record CreateInventoryRequest(@PositiveOrZero long totalQuantity) {}
