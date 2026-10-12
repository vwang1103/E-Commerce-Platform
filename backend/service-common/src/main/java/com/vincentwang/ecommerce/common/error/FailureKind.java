package com.vincentwang.ecommerce.common.error;

/** The kind of failure a request ran into, independent of how it is reported to the caller. */
public enum FailureKind {
    /** The request is well-formed but refers to something that cannot be used. */
    INVALID,
    NOT_FOUND,
    /** The request is valid but clashes with current state. */
    CONFLICT,
    /** A dependency is down; retrying later may succeed. */
    UNAVAILABLE
}
