package com.vincentwang.ecommerce.inventory.configuration;

import com.vincentwang.ecommerce.inventory.redis.IInventoryReservationStore;
import com.vincentwang.ecommerce.inventory.repository.IInventoryRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class InventoryRedisWarmupConfiguration {

    @Bean
    ApplicationRunner initializeRedisInventory(
            IInventoryRepository inventoryRepository, IInventoryReservationStore reservationStore) {
        return arguments -> inventoryRepository.findAllInventory().forEach(inventory -> {
            if (reservationStore.available(inventory.productId()) == null) {
                reservationStore.initialize(inventory.productId(), inventory.availableQuantity());
            }
        });
    }
}
