package com.vincentwang.ecommerce.inventory.controller;

import com.vincentwang.ecommerce.common.web.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Business failures are handled centrally; only this service's Redis dependency needs its own mapping. */
@RestControllerAdvice
public class RedisExceptionHandler {

    @ExceptionHandler(RedisConnectionFailureException.class)
    ProblemDetail handleRedisUnavailable(RedisConnectionFailureException exception, HttpServletRequest request) {
        return ProblemDetails.of(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Inventory reservation store unavailable",
                "Redis is unavailable; no reservation was accepted",
                request);
    }
}
