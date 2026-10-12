package com.vincentwang.ecommerce.order.controller.dto.request;

import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(@Positive long productId, @Positive long quantity) {}
