package com.vincentwang.ecommerce.inventory.service.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

public class ReservationNotFoundException extends AServiceException {

    public ReservationNotFoundException(long orderId) {
        super(
                FailureKind.NOT_FOUND,
                "Inventory resource not found",
                "Inventory reservation not found for order " + orderId);
    }
}
