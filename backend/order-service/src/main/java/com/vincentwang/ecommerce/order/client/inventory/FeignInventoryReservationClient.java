package com.vincentwang.ecommerce.order.client.inventory;

import com.vincentwang.ecommerce.order.client.inventory.dto.FeignInventoryReservationItem;
import com.vincentwang.ecommerce.order.client.inventory.dto.FeignInventoryTryRequest;
import com.vincentwang.ecommerce.order.model.InventoryReservationItem;
import com.vincentwang.ecommerce.order.service.exception.DependencyUnavailableException;
import com.vincentwang.ecommerce.order.service.exception.InventoryRejectedException;
import feign.FeignException;
import java.util.List;
import org.springframework.stereotype.Component;

/** Keeps Feign types out of the service layer by translating HTTP outcomes into business failures. */
@Component
public class FeignInventoryReservationClient implements IInventoryReservationClient {

    private final IInventoryFeignClient client;

    public FeignInventoryReservationClient(IInventoryFeignClient client) {
        this.client = client;
    }

    @Override
    public void tryReserveAll(long orderId, String userId, List<InventoryReservationItem> items, long ttlSeconds) {
        try {
            client.tryReservations(new FeignInventoryTryRequest(
                    orderId,
                    userId,
                    items.stream()
                            .map(item -> new FeignInventoryReservationItem(item.productId(), item.quantity()))
                            .toList(),
                    ttlSeconds));
        } catch (FeignException.NotFound | FeignException.Conflict exception) {
            throw new InventoryRejectedException();
        } catch (FeignException exception) {
            throw new DependencyUnavailableException("inventory", exception);
        }
    }

    @Override
    public void confirm(long orderId) {
        try {
            client.confirm(orderId);
        } catch (FeignException exception) {
            throw new DependencyUnavailableException("inventory", exception);
        }
    }

    @Override
    public void cancel(long orderId) {
        try {
            client.cancel(orderId);
        } catch (FeignException exception) {
            throw new DependencyUnavailableException("inventory", exception);
        }
    }
}
