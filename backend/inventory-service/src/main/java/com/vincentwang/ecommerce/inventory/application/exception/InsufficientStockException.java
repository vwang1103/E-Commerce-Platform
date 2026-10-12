package com.vincentwang.ecommerce.inventory.application.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

public class InsufficientStockException extends AServiceException {

    public InsufficientStockException(long productId) {
        super(FailureKind.CONFLICT, "Inventory conflict", "Insufficient stock for product " + productId);
    }
}
