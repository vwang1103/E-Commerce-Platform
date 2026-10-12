package com.vincentwang.ecommerce.order.infrastructure.persistence;

import com.vincentwang.ecommerce.order.domain.model.Order;
import com.vincentwang.ecommerce.order.domain.model.OrderItem;
import com.vincentwang.ecommerce.order.infrastructure.messaging.OrderOutboxEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IOrderMapper {
    int insertOrder(Order order);

    int insertItem(OrderItem item);

    Optional<Order> findById(@Param("userId") String userId, @Param("orderId") long orderId);

    Optional<Order> findByIdempotencyKey(@Param("userId") String userId, @Param("idempotencyKey") String key);

    List<Order> findByUserId(@Param("userId") String userId, @Param("offset") long offset, @Param("limit") int limit);

    long countByUserId(String userId);

    List<OrderItem> findItems(@Param("userId") String userId, @Param("orderId") long orderId);

    int markPendingPayment(@Param("userId") String userId, @Param("orderId") long orderId);

    int markConfirmingInventory(@Param("userId") String userId, @Param("orderId") long orderId);

    int markPaid(@Param("userId") String userId, @Param("orderId") long orderId);

    int markInventoryConfirmationFailed(@Param("userId") String userId, @Param("orderId") long orderId);

    List<Order> findExpiredForUpdate(@Param("limit") int limit);

    int markExpired(@Param("userId") String userId, @Param("orderId") long orderId);

    int insertProcessedEvent(
            @Param("userId") String userId,
            @Param("consumerName") String consumerName,
            @Param("eventId") String eventId);

    int insertOutbox(
            @Param("userId") String userId,
            @Param("eventId") String eventId,
            @Param("aggregateId") String aggregateId,
            @Param("eventType") String eventType,
            @Param("partitionKey") String partitionKey,
            @Param("payload") String payload,
            @Param("occurredAt") LocalDateTime occurredAt);

    List<OrderOutboxEvent> findUnpublishedOutbox(@Param("limit") int limit);

    int markOutboxPublished(@Param("userId") String userId, @Param("eventId") String eventId);
}
