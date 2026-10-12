package com.vincentwang.ecommerce.inventory.model;

import java.time.Instant;

public record Inventory(
        long productId,
        long totalQuantity,
        long availableQuantity,
        long reservedQuantity,
        long soldQuantity,
        long version,
        Instant updatedAt) {}
