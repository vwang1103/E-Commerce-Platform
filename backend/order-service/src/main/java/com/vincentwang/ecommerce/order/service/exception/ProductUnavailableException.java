package com.vincentwang.ecommerce.order.service.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

/** The product does not exist or is not on sale. */
public class ProductUnavailableException extends AServiceException {

    public ProductUnavailableException(long productId) {
        super(FailureKind.CONFLICT, "Product unavailable", "Product " + productId + " is not available for ordering");
    }
}
