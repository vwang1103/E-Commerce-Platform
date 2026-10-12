package com.vincentwang.ecommerce.order.application.exception;

import com.vincentwang.ecommerce.common.error.AServiceException;
import com.vincentwang.ecommerce.common.error.FailureKind;

/** A service this one depends on could not answer, so the request may be retried. */
public class DependencyUnavailableException extends AServiceException {

    public DependencyUnavailableException(String dependency, Throwable cause) {
        super(
                FailureKind.UNAVAILABLE,
                "Dependency unavailable",
                "The " + dependency + " service is temporarily unavailable",
                cause);
    }
}
