package com.vincentwang.ecommerce.inventory.controller.dto.request;

import com.vincentwang.ecommerce.common.web.PlatformHttpHeaders;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record TryReservationsRequest(
        @Positive long orderId,
        @Pattern(regexp = PlatformHttpHeaders.UUID_PATTERN) String userId,
        @NotEmpty @Size(max = 20) List<@Valid ReservationItemRequest> items,
        @Min(1) @Max(3600) Long ttlSeconds) {}
