package com.vincentwang.ecommerce.inventory.scheduler;

import com.vincentwang.ecommerce.inventory.service.IInventoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically releases inventory reservations whose payment window has elapsed. */
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
