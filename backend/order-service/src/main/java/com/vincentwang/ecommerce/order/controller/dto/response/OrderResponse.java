package com.vincentwang.ecommerce.order.controller.dto.response;

import com.vincentwang.ecommerce.order.model.Order;
import com.vincentwang.ecommerce.order.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

public record OrderResponse(
        @JsonSerialize(using = ToStringSerializer.class) long orderId,
        String userId,
        OrderStatus status,
        long totalMinor,
        String currency,
        LocalDateTime paymentDeadline,
        long version,
        LocalDateTime createdAt,
        List<OrderItemResponse> items) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalMinor(),
                order.getCurrency(),
                order.getPaymentDeadline(),
                order.getVersion(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
