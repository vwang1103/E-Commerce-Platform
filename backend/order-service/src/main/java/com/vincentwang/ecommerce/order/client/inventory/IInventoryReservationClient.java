package com.vincentwang.ecommerce.order.client.inventory;

import com.vincentwang.ecommerce.order.model.InventoryReservationItem;
import java.util.List;

public interface IInventoryReservationClient {

    void tryReserveAll(long orderId, String userId, List<InventoryReservationItem> items, long ttlSeconds);

    void confirm(long orderId);

    void cancel(long orderId);
}
