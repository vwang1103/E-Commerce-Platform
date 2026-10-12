package com.vincentwang.ecommerce.inventory.api.dto.request;

import com.vincentwang.ecommerce.common.web.PlatformHttpHeaders;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TryReservationRequest(
        @Positive long orderId,
        @Pattern(regexp = PlatformHttpHeaders.UUID_PATTERN) String userId,
        @Positive long productId,
        @Positive long quantity,
        @Min(1) @Max(3600) Long ttlSeconds) {}
