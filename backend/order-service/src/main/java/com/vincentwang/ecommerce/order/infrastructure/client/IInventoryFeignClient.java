package com.vincentwang.ecommerce.order.infrastructure.client;

import com.vincentwang.ecommerce.order.infrastructure.client.dto.FeignInventoryReservationResponse;
import com.vincentwang.ecommerce.order.infrastructure.client.dto.FeignInventoryTryRequest;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", url = "${order.inventory.service-url:}")
public interface IInventoryFeignClient {
    @PostMapping("/api/v1/inventory/reservations/try-all")
    List<FeignInventoryReservationResponse> tryReservations(@RequestBody FeignInventoryTryRequest request);

    @PostMapping("/api/v1/inventory/reservations/{orderId}/confirm-all")
    List<FeignInventoryReservationResponse> confirm(@PathVariable long orderId);

    @PostMapping("/api/v1/inventory/reservations/{orderId}/cancel-all")
    List<FeignInventoryReservationResponse> cancel(@PathVariable long orderId);
}
