package com.vincentwang.ecommerce.order.repository;

import com.vincentwang.ecommerce.order.model.Order;
import com.vincentwang.ecommerce.order.model.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IOrderRepository {

    int insertOrder(Order order);

    int insertItem(OrderItem item);

    Optional<Order> findById(String userId, long orderId);

    Optional<Order> findByIdempotencyKey(String userId, String key);

    List<Order> findByUserId(String userId, long offset, int limit);

    long countByUserId(String userId);

    List<OrderItem> findItems(String userId, long orderId);

    int markPendingPayment(String userId, long orderId);

    int markConfirmingInventory(String userId, long orderId);

    int markPaid(String userId, long orderId);

    int markInventoryConfirmationFailed(String userId, long orderId);

    List<Order> findExpiredForUpdate(int limit);

    int markExpired(String userId, long orderId);

    int insertProcessedEvent(String userId, String consumerName, String eventId);

    int appendOutboxEvent(
            String userId,
            String eventId,
            String aggregateId,
            String eventType,
            String partitionKey,
            String payload,
            LocalDateTime occurredAt);
}
