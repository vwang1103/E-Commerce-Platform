package com.vincentwang.ecommerce.order.service.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

/** The inventory service answered and declined: no stock record, or not enough stock. */
public class InventoryRejectedException extends AServiceException {

    public InventoryRejectedException() {
        super(FailureKind.CONFLICT, "Inventory reservation failed", "Not enough stock is available for this item");
    }
}
