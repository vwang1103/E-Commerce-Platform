package com.vincentwang.ecommerce.order.service;

import com.vincentwang.ecommerce.common.pagination.PageResult;
import com.vincentwang.ecommerce.order.controller.dto.request.CreateOrderRequest;
import com.vincentwang.ecommerce.order.controller.dto.response.OrderResponse;

/** Application contract for order lifecycle and asynchronous workflow use cases. */
public interface IOrderService {

    OrderResponse submit(String userId, CreateOrderRequest request, String idempotencyKey);

    OrderResponse get(String userId, long orderId);

    PageResult<OrderResponse> list(String userId, int page, int size);

    OrderResponse pay(String userId, long orderId, String idempotencyKey);

    void activate(String eventId, String userId, long orderId);

    void applyInventoryCommand(String eventId, String eventType, String userId, long orderId);

    void markInventoryConfirmationFailed(String userId, long orderId);

    int expireBatch(int batchSize);
}
