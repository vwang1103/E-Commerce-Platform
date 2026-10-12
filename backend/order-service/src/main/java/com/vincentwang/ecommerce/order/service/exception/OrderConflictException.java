package com.vincentwang.ecommerce.order.service.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

public class OrderConflictException extends AServiceException {

    public OrderConflictException(String message) {
        super(FailureKind.CONFLICT, "Order state conflict", message);
    }
}
