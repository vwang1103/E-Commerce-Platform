package com.vincentwang.ecommerce.inventory.application.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

public class InventoryConflictException extends AServiceException {

    public InventoryConflictException(String message) {
        super(FailureKind.CONFLICT, "Inventory conflict", message);
    }
}
