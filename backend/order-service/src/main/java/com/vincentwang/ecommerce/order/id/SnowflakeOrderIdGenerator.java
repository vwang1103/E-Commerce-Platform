package com.vincentwang.ecommerce.order.id;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Generates sortable IDs from time, worker identity, and an in-millisecond sequence. */
@Component
public class SnowflakeOrderIdGenerator implements IOrderIdGenerator {
    private static final long EPOCH = 1_767_225_600_000L;
    private static final long MAX_SEQUENCE = 4095;

    private final Clock clock = Clock.systemUTC();
    private final long workerId;
    private long lastTimestamp = -1;
    private long sequence;

    public SnowflakeOrderIdGenerator(@Value("${ORDER_ID_WORKER_ID:0}") long workerId) {
        if (workerId < 0 || workerId > 1023) {
            throw new IllegalArgumentException("ORDER_ID_WORKER_ID must be between 0 and 1023");
        }
        this.workerId = workerId;
    }

    @Override
    public synchronized long nextId() {
        long timestamp = clock.millis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("System clock moved backwards");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            while (sequence == 0 && timestamp <= lastTimestamp) {
                timestamp = clock.millis();
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << 22) | (workerId << 12) | sequence;
    }
}
