package com.vincentwang.ecommerce.inventory.controller.dto.request;

import jakarta.validation.constraints.Positive;

public record ReservationItemRequest(@Positive long productId, @Positive long quantity) {}
