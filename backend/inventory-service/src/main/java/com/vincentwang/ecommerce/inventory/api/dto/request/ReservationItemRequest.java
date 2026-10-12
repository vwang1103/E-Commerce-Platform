package com.vincentwang.ecommerce.inventory.api.dto.request;

import jakarta.validation.constraints.Positive;

public record ReservationItemRequest(@Positive long productId, @Positive long quantity) {}
