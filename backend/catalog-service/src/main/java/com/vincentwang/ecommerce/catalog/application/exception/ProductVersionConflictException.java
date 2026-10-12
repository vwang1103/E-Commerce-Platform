package com.vincentwang.ecommerce.catalog.application.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

public class ProductVersionConflictException extends AServiceException {

    public ProductVersionConflictException(long productId) {
        super(
                FailureKind.CONFLICT,
                "Product conflict",
                "Product " + productId + " was modified; reload it before updating");
    }
}
