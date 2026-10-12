package com.vincentwang.ecommerce.catalog.application.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

public class InvalidCategoryException extends AServiceException {

    public InvalidCategoryException(long categoryId) {
        super(FailureKind.INVALID, "Invalid category", "Category " + categoryId + " does not exist");
    }
}
