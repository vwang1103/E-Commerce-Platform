package com.vincentwang.ecommerce.common.error;

/**
 * Base for every business failure a service reports to its callers. The kind decides the HTTP status
 * once, in {@code ServiceExceptionHandler}, so no service writes its own mapping.
 */
public abstract class AServiceException extends RuntimeException {

    private final FailureKind kind;
    private final String title;

    protected AServiceException(FailureKind kind, String title, String message) {
        super(message);
        this.kind = kind;
        this.title = title;
    }

    protected AServiceException(FailureKind kind, String title, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.title = title;
    }

    public FailureKind kind() {
        return kind;
    }

    /** A short, stable summary suitable for a problem-detail title. */
    public String title() {
        return title;
    }
}
