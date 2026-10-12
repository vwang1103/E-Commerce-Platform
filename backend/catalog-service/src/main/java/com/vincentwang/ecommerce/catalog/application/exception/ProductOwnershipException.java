package com.vincentwang.ecommerce.catalog.application.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

public class ProductOwnershipException extends AServiceException {

    public ProductOwnershipException(long productId) {
        super(
                FailureKind.CONFLICT,
                "Product ownership conflict",
                "Only the product owner can update product " + productId);
    }
}
