package com.vincentwang.ecommerce.inventory.repository.mybatis;

import com.vincentwang.ecommerce.inventory.model.Inventory;
import com.vincentwang.ecommerce.inventory.model.InventoryReservation;
import com.vincentwang.ecommerce.inventory.model.InventorySyncOperation;
import com.vincentwang.ecommerce.inventory.model.InventorySyncTask;
import com.vincentwang.ecommerce.inventory.repository.IInventoryRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisInventoryRepository implements IInventoryRepository {

    private final IInventoryMapper mapper;

    public MyBatisInventoryRepository(IInventoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public int insertInventory(long productId, long totalQuantity) {
        return mapper.insertInventory(productId, totalQuantity);
    }

    @Override
    public Inventory findInventory(long productId) {
        return mapper.findInventory(productId);
    }

    @Override
    public List<Inventory> findInventories(Collection<Long> productIds) {
        return productIds.isEmpty() ? List.of() : mapper.findInventories(productIds);
    }

    @Override
    public int reserveStock(long productId, long quantity) {
        return mapper.reserveStock(productId, quantity);
    }

    @Override
    public int confirmStock(long productId, long quantity) {
        return mapper.confirmStock(productId, quantity);
    }

    @Override
    public int cancelStock(long productId, long quantity) {
        return mapper.cancelStock(productId, quantity);
    }

    @Override
    public int insertReservation(InventoryReservation reservation) {
        return mapper.insertReservation(reservation);
    }

    @Override
    public InventoryReservation findReservation(long orderId) {
        return mapper.findReservation(orderId);
    }

    @Override
    public List<InventoryReservation> findReservations(long orderId) {
        return mapper.findReservations(orderId);
    }

    @Override
    public int markConfirmed(long orderId, long productId) {
        return mapper.markConfirmed(orderId, productId);
    }

    @Override
    public int markCancelled(long orderId, long productId) {
        return mapper.markCancelled(orderId, productId);
    }

    @Override
    public int enqueueInventorySync(long orderId, long productId, InventorySyncOperation operation) {
        return mapper.enqueueInventorySync(orderId, productId, operation);
    }

    @Override
    public List<InventorySyncTask> findPendingInventorySync(int limit) {
        return mapper.findPendingInventorySync(limit);
    }

    @Override
    public int markInventorySyncApplied(long syncId) {
        return mapper.markInventorySyncApplied(syncId);
    }

    @Override
    public int markInventorySyncFailed(long syncId, String error) {
        return mapper.markInventorySyncFailed(syncId, error);
    }

    @Override
    public List<InventoryReservation> findExpiredTryReservations(Instant now, int limit) {
        return mapper.findExpiredTryReservations(now, limit);
    }

    @Override
    public List<Inventory> findAllInventory() {
        return mapper.findAllInventory();
    }
}
