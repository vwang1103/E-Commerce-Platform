package com.vincentwang.ecommerce.inventory.api.dto.request;

import com.vincentwang.ecommerce.common.web.PlatformHttpHeaders;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TryReservationsRequest(
        @Positive long orderId,
        @Pattern(regexp = PlatformHttpHeaders.UUID_PATTERN) String userId,
        @NotEmpty @Size(max = 20) List<@Valid ReservationItemRequest> items,
        @Min(1) @Max(3600) Long ttlSeconds) {}
