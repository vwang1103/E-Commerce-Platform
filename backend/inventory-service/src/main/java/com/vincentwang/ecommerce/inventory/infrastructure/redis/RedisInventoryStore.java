package com.vincentwang.ecommerce.inventory.infrastructure.redis;

import com.vincentwang.ecommerce.inventory.application.IInventoryReservationStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisInventoryStore implements IInventoryReservationStore {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> tryScript;
    private final RedisScript<Long> confirmScript;
    private final RedisScript<Long> cancelScript;

    public RedisInventoryStore(
            StringRedisTemplate redisTemplate,
            @Qualifier("inventoryTryScript") RedisScript<Long> tryScript,
            @Qualifier("inventoryConfirmScript") RedisScript<Long> confirmScript,
            @Qualifier("inventoryCancelScript") RedisScript<Long> cancelScript) {
        this.redisTemplate = redisTemplate;
        this.tryScript = tryScript;
        this.confirmScript = confirmScript;
        this.cancelScript = cancelScript;
    }

    @Override
    public void initialize(long productId, long availableQuantity) {
        redisTemplate.opsForValue().set(availableKey(productId), Long.toString(availableQuantity));
    }

    @Override
    public long tryReserve(
            long productId, long orderId, String userId, long quantity, Instant expiresAt, Duration redisTtl) {
        return Objects.requireNonNull(redisTemplate.execute(
                tryScript,
                List.of(availableKey(productId), reservationKey(productId, orderId)),
                userId,
                Long.toString(quantity),
                Long.toString(expiresAt.toEpochMilli()),
                Long.toString(redisTtl.toMillis())));
    }

    @Override
    public long confirm(long productId, long orderId, Duration retention) {
        return Objects.requireNonNull(redisTemplate.execute(
                confirmScript, List.of(reservationKey(productId, orderId)), Long.toString(retention.toMillis())));
    }

    @Override
    public long cancel(long productId, long orderId, Duration retention) {
        return Objects.requireNonNull(redisTemplate.execute(
                cancelScript,
                List.of(availableKey(productId), reservationKey(productId, orderId)),
                Long.toString(retention.toMillis())));
    }

    @Override
    public Long available(long productId) {
        String value = redisTemplate.opsForValue().get(availableKey(productId));
        return value == null ? null : Long.valueOf(value);
    }

    static String availableKey(long productId) {
        return "inventory:{" + productId + "}:available";
    }

    static String reservationKey(long productId, long orderId) {
        return "inventory:{" + productId + "}:reservation:" + orderId;
    }
}
