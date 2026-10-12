package com.vincentwang.ecommerce.inventory.service;

import com.vincentwang.ecommerce.inventory.controller.dto.request.CreateInventoryRequest;
import com.vincentwang.ecommerce.inventory.controller.dto.request.TryReservationRequest;
import com.vincentwang.ecommerce.inventory.controller.dto.request.TryReservationsRequest;
import com.vincentwang.ecommerce.inventory.controller.dto.response.InventoryResponse;
import com.vincentwang.ecommerce.inventory.controller.dto.response.ReservationResponse;
import java.util.Collection;
import java.util.List;

/** Application contract for stock and TCC reservation use cases. */
public interface IInventoryService {

    InventoryResponse createInventory(long productId, CreateInventoryRequest request);

    InventoryResponse getInventory(long productId);

    /** Reads stock for several products at once; product IDs without a stock record are omitted. */
    List<InventoryResponse> listInventory(Collection<Long> productIds);

    ReservationResponse tryReserve(TryReservationRequest request);

    List<ReservationResponse> tryReserveAll(TryReservationsRequest request);

    ReservationResponse confirm(long orderId);

    List<ReservationResponse> confirmAll(long orderId);

    ReservationResponse cancel(long orderId);

    List<ReservationResponse> cancelAll(long orderId);

    ReservationResponse getReservation(long orderId);

    List<ReservationResponse> getReservations(long orderId);

    int synchronizeRedis(int batchSize);

    int cancelExpiredReservations(int batchSize);

    void detectDrift();
}
