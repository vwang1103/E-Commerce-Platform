package com.vincentwang.ecommerce.inventory.infrastructure.scheduler;

import com.vincentwang.ecommerce.inventory.application.IInventoryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryDriftDetector {

    private final IInventoryService inventoryService;

    public InventoryDriftDetector(IInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Scheduled(
            initialDelayString = "${INVENTORY_DRIFT_CHECK_INITIAL_DELAY:5s}",
            fixedDelayString = "${INVENTORY_DRIFT_CHECK_DELAY:30s}")
    public void detectDrift() {
        inventoryService.detectDrift();
    }
}
