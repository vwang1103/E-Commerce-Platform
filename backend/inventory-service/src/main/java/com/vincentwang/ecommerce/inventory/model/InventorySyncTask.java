package com.vincentwang.ecommerce.inventory.model;

public record InventorySyncTask(long syncId, long orderId, long productId, InventorySyncOperation operation) {}
