package com.vincentwang.ecommerce.catalog.application.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

public class ProductNotFoundException extends AServiceException {

    public ProductNotFoundException(long productId) {
        super(FailureKind.NOT_FOUND, "Product not found", "Product " + productId + " was not found");
    }
}
