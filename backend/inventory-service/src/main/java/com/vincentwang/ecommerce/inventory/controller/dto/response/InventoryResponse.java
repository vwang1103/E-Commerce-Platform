package com.vincentwang.ecommerce.inventory.controller.dto.response;

import com.vincentwang.ecommerce.inventory.model.Inventory;
import java.time.Instant;

public record InventoryResponse(
        long productId,
        long totalQuantity,
        long availableQuantity,
        long reservedQuantity,
        long soldQuantity,
        long version,
        Instant updatedAt) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.productId(),
                inventory.totalQuantity(),
                inventory.availableQuantity(),
                inventory.reservedQuantity(),
                inventory.soldQuantity(),
                inventory.version(),
                inventory.updatedAt());
    }
}
