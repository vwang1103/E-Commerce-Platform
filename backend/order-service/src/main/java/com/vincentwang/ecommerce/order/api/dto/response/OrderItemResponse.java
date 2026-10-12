package com.vincentwang.ecommerce.order.api.dto.response;

import com.vincentwang.ecommerce.order.domain.model.OrderItem;

public record OrderItemResponse(long productId, String productName, long unitPriceMinor, long quantity) {
    static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(), item.getProductName(), item.getUnitPriceMinor(), item.getQuantity());
    }
}
