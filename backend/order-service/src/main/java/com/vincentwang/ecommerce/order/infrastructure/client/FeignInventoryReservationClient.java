package com.vincentwang.ecommerce.order.infrastructure.client;

import com.vincentwang.ecommerce.order.application.IInventoryReservationClient;
import com.vincentwang.ecommerce.order.application.InventoryReservationItem;
import com.vincentwang.ecommerce.order.application.exception.DependencyUnavailableException;
import com.vincentwang.ecommerce.order.application.exception.InventoryRejectedException;
import com.vincentwang.ecommerce.order.infrastructure.client.dto.FeignInventoryReservationItem;
import com.vincentwang.ecommerce.order.infrastructure.client.dto.FeignInventoryTryRequest;
import feign.FeignException;
import java.util.List;
import org.springframework.stereotype.Component;

/** Keeps Feign types out of the application layer by translating HTTP outcomes into domain failures. */
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
