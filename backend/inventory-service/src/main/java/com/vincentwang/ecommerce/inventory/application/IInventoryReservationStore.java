package com.vincentwang.ecommerce.inventory.application;

import java.time.Duration;
import java.time.Instant;

public interface IInventoryReservationStore {

    void initialize(long productId, long availableQuantity);

    long tryReserve(long productId, long orderId, String userId, long quantity, Instant expiresAt, Duration redisTtl);

    long confirm(long productId, long orderId, Duration retention);

    long cancel(long productId, long orderId, Duration retention);

    Long available(long productId);
}
