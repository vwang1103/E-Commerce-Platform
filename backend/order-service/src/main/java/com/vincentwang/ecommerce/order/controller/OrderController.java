package com.vincentwang.ecommerce.order.controller;

import com.vincentwang.ecommerce.common.web.ApiPageResponse;
import com.vincentwang.ecommerce.common.web.PlatformHttpHeaders;
import com.vincentwang.ecommerce.order.controller.dto.request.CreateOrderRequest;
import com.vincentwang.ecommerce.order.controller.dto.response.OrderResponse;
import com.vincentwang.ecommerce.order.service.IOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** Public HTTP boundary for placing, viewing, and paying orders. */
@Validated
@RestController
@RequestMapping("/api/v1")
public class OrderController {
    private final IOrderService orderService;

    public OrderController(IOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> create(
            @RequestHeader(PlatformHttpHeaders.USER_ID) @Pattern(regexp = PlatformHttpHeaders.UUID_PATTERN) String userId,
            @RequestHeader(PlatformHttpHeaders.IDEMPOTENCY_KEY) @NotBlank @Size(max = 128) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.submit(userId, request, idempotencyKey);
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/orders/" + response.orderId()))
                .body(response);
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse get(
            @RequestHeader(PlatformHttpHeaders.USER_ID) @Pattern(regexp = PlatformHttpHeaders.UUID_PATTERN) String userId,
            @PathVariable @Positive long orderId) {
        return orderService.get(userId, orderId);
    }

    @GetMapping("/orders")
    public ApiPageResponse<OrderResponse> list(
            @RequestHeader(PlatformHttpHeaders.USER_ID) @Pattern(regexp = PlatformHttpHeaders.UUID_PATTERN) String userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = orderService.list(userId, page, size);
        return ApiPageResponse.of(result.items(), result.page(), result.size(), result.totalElements());
    }

    @PostMapping("/orders/{orderId}/payment")
    public OrderResponse pay(
            @PathVariable @Positive long orderId,
            @RequestHeader(PlatformHttpHeaders.USER_ID) @Pattern(regexp = PlatformHttpHeaders.UUID_PATTERN) String userId,
            @RequestHeader(PlatformHttpHeaders.IDEMPOTENCY_KEY) @NotBlank @Size(max = 128) String idempotencyKey) {
        return orderService.pay(userId, orderId, idempotencyKey);
    }
}
