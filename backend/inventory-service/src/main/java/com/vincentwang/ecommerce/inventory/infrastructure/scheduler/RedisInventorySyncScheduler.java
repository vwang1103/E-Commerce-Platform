package com.vincentwang.ecommerce.inventory.infrastructure.scheduler;

import com.vincentwang.ecommerce.inventory.application.IInventoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RedisInventorySyncScheduler {
    private final IInventoryService inventoryService;
    private final int batchSize;

    public RedisInventorySyncScheduler(
            IInventoryService inventoryService, @Value("${inventory.redis-sync.batch-size:100}") int batchSize) {
        this.inventoryService = inventoryService;
        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString = "${inventory.redis-sync.fixed-delay:500ms}",
            initialDelayString = "${inventory.redis-sync.initial-delay:500ms}")
    public void synchronize() {
        inventoryService.synchronizeRedis(batchSize);
    }
}
