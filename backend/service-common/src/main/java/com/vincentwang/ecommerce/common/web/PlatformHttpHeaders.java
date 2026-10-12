package com.vincentwang.ecommerce.common.web;

public final class PlatformHttpHeaders {

    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String USER_ID = "X-User-Id";
    public static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    private PlatformHttpHeaders() {}
}
