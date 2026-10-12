package com.vincentwang.ecommerce.inventory.service.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

public class InventoryNotFoundException extends AServiceException {

    public InventoryNotFoundException(long productId) {
        super(FailureKind.NOT_FOUND, "Inventory resource not found", "Inventory not found for product " + productId);
    }
}
