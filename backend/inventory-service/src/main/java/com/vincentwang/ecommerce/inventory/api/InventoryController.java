package com.vincentwang.ecommerce.inventory.api;

import com.vincentwang.ecommerce.inventory.api.dto.request.CreateInventoryRequest;
import com.vincentwang.ecommerce.inventory.api.dto.request.TryReservationRequest;
import com.vincentwang.ecommerce.inventory.api.dto.request.TryReservationsRequest;
import com.vincentwang.ecommerce.inventory.api.dto.response.InventoryResponse;
import com.vincentwang.ecommerce.inventory.api.dto.response.ReservationResponse;
import com.vincentwang.ecommerce.inventory.application.IInventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final IInventoryService inventoryService;

    public InventoryController(IInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PutMapping("/{productId}")
    public ResponseEntity<InventoryResponse> createInventory(
            @PathVariable @Positive long productId, @Valid @RequestBody CreateInventoryRequest request) {
        InventoryResponse response = inventoryService.createInventory(productId, request);
        return ResponseEntity.created(URI.create("/api/v1/inventory/" + response.productId()))
                .body(response);
    }

    @GetMapping
    public List<InventoryResponse> listInventory(@RequestParam List<@Positive Long> productIds) {
        return inventoryService.listInventory(productIds);
    }

    @GetMapping("/{productId}")
    public InventoryResponse getInventory(@PathVariable @Positive long productId) {
        return inventoryService.getInventory(productId);
    }

    @PostMapping("/reservations/try")
    public ReservationResponse tryReservation(@Valid @RequestBody TryReservationRequest request) {
        return inventoryService.tryReserve(request);
    }

    @PostMapping("/reservations/try-all")
    public List<ReservationResponse> tryReservations(@Valid @RequestBody TryReservationsRequest request) {
        return inventoryService.tryReserveAll(request);
    }

    @PostMapping("/reservations/{orderId}/confirm")
    public ReservationResponse confirm(@PathVariable @Positive long orderId) {
        return inventoryService.confirm(orderId);
    }

    @PostMapping("/reservations/{orderId}/confirm-all")
    public List<ReservationResponse> confirmAll(@PathVariable @Positive long orderId) {
        return inventoryService.confirmAll(orderId);
    }

    @PostMapping("/reservations/{orderId}/cancel")
    public ReservationResponse cancel(@PathVariable @Positive long orderId) {
        return inventoryService.cancel(orderId);
    }

    @PostMapping("/reservations/{orderId}/cancel-all")
    public List<ReservationResponse> cancelAll(@PathVariable @Positive long orderId) {
        return inventoryService.cancelAll(orderId);
    }

    @GetMapping("/reservations/{orderId}")
    public ReservationResponse getReservation(@PathVariable @Positive long orderId) {
        return inventoryService.getReservation(orderId);
    }

    @GetMapping("/reservations/{orderId}/all")
    public List<ReservationResponse> getReservations(@PathVariable @Positive long orderId) {
        return inventoryService.getReservations(orderId);
    }
}
