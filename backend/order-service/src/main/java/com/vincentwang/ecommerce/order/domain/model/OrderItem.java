package com.vincentwang.ecommerce.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private Long orderItemId;
    private long orderId;
    private String userId;
    private long productId;
    private String productName;
    private long unitPriceMinor;
    private long quantity;
}
