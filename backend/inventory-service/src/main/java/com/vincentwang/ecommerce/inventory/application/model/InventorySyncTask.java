package com.vincentwang.ecommerce.inventory.application.model;

public record InventorySyncTask(long syncId, long orderId, long productId, InventorySyncOperation operation) {}
