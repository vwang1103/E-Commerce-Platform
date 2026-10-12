package com.vincentwang.ecommerce.order.service.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

public class OrderNotFoundException extends AServiceException {

    public OrderNotFoundException(long orderId) {
        super(FailureKind.NOT_FOUND, "Order not found", "Order " + orderId + " was not found");
    }
}
