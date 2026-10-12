package com.vincentwang.ecommerce.order.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private long orderId;
    private String userId;
    private OrderStatus status;
    private long totalMinor;
    private String currency;
    private String idempotencyKey;
    private LocalDateTime paymentDeadline;
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItem> items;
}
