package com.vincentwang.ecommerce.inventory.infrastructure.scheduler;

import com.vincentwang.ecommerce.inventory.application.IInventoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiredReservationScheduler {

    private final IInventoryService inventoryService;
    private final int batchSize;

    public ExpiredReservationScheduler(
            IInventoryService inventoryService, @Value("${inventory.reservation.expiry-batch-size}") int batchSize) {
        this.inventoryService = inventoryService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${inventory.reservation.expiry-delay}")
    public void cancelExpiredReservations() {
        inventoryService.cancelExpiredReservations(batchSize);
    }
}
