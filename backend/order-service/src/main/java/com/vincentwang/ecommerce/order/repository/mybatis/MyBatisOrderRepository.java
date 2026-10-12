package com.vincentwang.ecommerce.order.repository.mybatis;

import com.vincentwang.ecommerce.order.model.Order;
import com.vincentwang.ecommerce.order.model.OrderItem;
import com.vincentwang.ecommerce.order.repository.IOrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisOrderRepository implements IOrderRepository {

    private final IOrderMapper mapper;

    public MyBatisOrderRepository(IOrderMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public int insertOrder(Order order) {
        return mapper.insertOrder(order);
    }

    @Override
    public int insertItem(OrderItem item) {
        return mapper.insertItem(item);
    }

    @Override
    public Optional<Order> findById(String userId, long orderId) {
        return mapper.findById(userId, orderId);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String userId, String key) {
        return mapper.findByIdempotencyKey(userId, key);
    }

    @Override
    public List<Order> findByUserId(String userId, long offset, int limit) {
        return mapper.findByUserId(userId, offset, limit);
    }

    @Override
    public long countByUserId(String userId) {
        return mapper.countByUserId(userId);
    }

    @Override
    public List<OrderItem> findItems(String userId, long orderId) {
        return mapper.findItems(userId, orderId);
    }

    @Override
    public int markPendingPayment(String userId, long orderId) {
        return mapper.markPendingPayment(userId, orderId);
    }

    @Override
    public int markPaid(String userId, long orderId) {
        return mapper.markPaid(userId, orderId);
    }

    @Override
    public int markConfirmingInventory(String userId, long orderId) {
        return mapper.markConfirmingInventory(userId, orderId);
    }

    @Override
    public int markInventoryConfirmationFailed(String userId, long orderId) {
        return mapper.markInventoryConfirmationFailed(userId, orderId);
    }

    @Override
    public List<Order> findExpiredForUpdate(int limit) {
        return mapper.findExpiredForUpdate(limit);
    }

    @Override
    public int markExpired(String userId, long orderId) {
        return mapper.markExpired(userId, orderId);
    }

    @Override
    public int insertProcessedEvent(String userId, String consumerName, String eventId) {
        return mapper.insertProcessedEvent(userId, consumerName, eventId);
    }

    @Override
    public int appendOutboxEvent(
            String userId,
            String eventId,
            String aggregateId,
            String eventType,
            String partitionKey,
            String payload,
            LocalDateTime occurredAt) {
        return mapper.insertOutbox(userId, eventId, aggregateId, eventType, partitionKey, payload, occurredAt);
    }
}
