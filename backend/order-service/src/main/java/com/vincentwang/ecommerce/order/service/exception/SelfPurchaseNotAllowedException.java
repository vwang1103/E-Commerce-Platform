package com.vincentwang.ecommerce.order.service.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

public class SelfPurchaseNotAllowedException extends AServiceException {

    public SelfPurchaseNotAllowedException(long productId) {
        super(
                FailureKind.CONFLICT,
                "Self-purchase is not allowed",
                "You cannot purchase your own product " + productId);
    }
}
