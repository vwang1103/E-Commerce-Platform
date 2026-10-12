package com.vincentwang.ecommerce.inventory.infrastructure.persistence;

import com.vincentwang.ecommerce.inventory.application.model.InventorySyncOperation;
import com.vincentwang.ecommerce.inventory.application.model.InventorySyncTask;
import com.vincentwang.ecommerce.inventory.domain.model.Inventory;
import com.vincentwang.ecommerce.inventory.domain.model.InventoryReservation;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IInventoryMapper {

    int insertInventory(@Param("productId") long productId, @Param("totalQuantity") long totalQuantity);

    Inventory findInventory(@Param("productId") long productId);

    List<Inventory> findInventories(@Param("productIds") Collection<Long> productIds);

    int reserveStock(@Param("productId") long productId, @Param("quantity") long quantity);

    int confirmStock(@Param("productId") long productId, @Param("quantity") long quantity);

    int cancelStock(@Param("productId") long productId, @Param("quantity") long quantity);

    int insertReservation(InventoryReservation reservation);

    InventoryReservation findReservation(@Param("orderId") long orderId);

    List<InventoryReservation> findReservations(@Param("orderId") long orderId);

    int markConfirmed(@Param("orderId") long orderId, @Param("productId") long productId);

    int markCancelled(@Param("orderId") long orderId, @Param("productId") long productId);

    int enqueueInventorySync(
            @Param("orderId") long orderId,
            @Param("productId") long productId,
            @Param("operation") InventorySyncOperation operation);

    List<InventorySyncTask> findPendingInventorySync(@Param("limit") int limit);

    int markInventorySyncApplied(@Param("syncId") long syncId);

    int markInventorySyncFailed(@Param("syncId") long syncId, @Param("error") String error);

    List<InventoryReservation> findExpiredTryReservations(@Param("now") Instant now, @Param("limit") int limit);

    List<Inventory> findAllInventory();
}
