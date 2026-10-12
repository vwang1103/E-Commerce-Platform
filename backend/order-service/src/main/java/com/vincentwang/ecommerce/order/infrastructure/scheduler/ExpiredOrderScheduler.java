package com.vincentwang.ecommerce.order.infrastructure.scheduler;

import com.vincentwang.ecommerce.order.application.IOrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "order.expiry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExpiredOrderScheduler {
    private final IOrderService orderService;
    private final int batchSize;

    public ExpiredOrderScheduler(IOrderService orderService, @Value("${order.expiry.batch-size}") int batchSize) {
        this.orderService = orderService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${order.expiry.poll-delay}")
    public void expireOrders() {
        orderService.expireBatch(batchSize);
    }
}
