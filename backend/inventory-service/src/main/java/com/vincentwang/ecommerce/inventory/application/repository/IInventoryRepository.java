package com.vincentwang.ecommerce.inventory.application.repository;

import com.vincentwang.ecommerce.inventory.application.model.InventorySyncOperation;
import com.vincentwang.ecommerce.inventory.application.model.InventorySyncTask;
import com.vincentwang.ecommerce.inventory.domain.model.Inventory;
import com.vincentwang.ecommerce.inventory.domain.model.InventoryReservation;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface IInventoryRepository {

    int insertInventory(long productId, long totalQuantity);

    Inventory findInventory(long productId);

    List<Inventory> findInventories(Collection<Long> productIds);

    int reserveStock(long productId, long quantity);

    int confirmStock(long productId, long quantity);

    int cancelStock(long productId, long quantity);

    int insertReservation(InventoryReservation reservation);

    InventoryReservation findReservation(long orderId);

    List<InventoryReservation> findReservations(long orderId);

    int markConfirmed(long orderId, long productId);

    int markCancelled(long orderId, long productId);

    int enqueueInventorySync(long orderId, long productId, InventorySyncOperation operation);

    List<InventorySyncTask> findPendingInventorySync(int limit);

    int markInventorySyncApplied(long syncId);

    int markInventorySyncFailed(long syncId, String error);

    List<InventoryReservation> findExpiredTryReservations(Instant now, int limit);

    List<Inventory> findAllInventory();
}
