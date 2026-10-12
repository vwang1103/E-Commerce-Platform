package com.vincentwang.ecommerce.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vincentwang.ecommerce.inventory.api.dto.request.TryReservationRequest;
import com.vincentwang.ecommerce.inventory.application.model.InventorySyncOperation;
import com.vincentwang.ecommerce.inventory.application.model.InventorySyncTask;
import com.vincentwang.ecommerce.inventory.application.repository.IInventoryRepository;
import com.vincentwang.ecommerce.inventory.domain.model.Inventory;
import com.vincentwang.ecommerce.inventory.domain.model.InventoryReservation;
import com.vincentwang.ecommerce.inventory.domain.model.ReservationStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;

class InventoryApplicationServiceTest {

    private static final String USER_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";

    private final IInventoryRepository inventoryRepository = mock(IInventoryRepository.class);
    private final IInventoryReservationStore reservationStore = mock(IInventoryReservationStore.class);
    private final IInventoryService inventoryService = new InventoryApplicationService(
            inventoryRepository, reservationStore, Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(24));

    @Test
    void rejectsTryWithoutChangingMysqlWhenRedisIsUnavailable() {
        when(inventoryRepository.findReservations(42)).thenReturn(List.of());
        doThrow(new RedisConnectionFailureException("Redis unavailable"))
                .when(reservationStore)
                .tryReserve(
                        org.mockito.ArgumentMatchers.eq(101L),
                        org.mockito.ArgumentMatchers.eq(42L),
                        org.mockito.ArgumentMatchers.eq(USER_ID),
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> inventoryService.tryReserve(new TryReservationRequest(42, USER_ID, 101, 1, 60L)))
                .isInstanceOf(RedisConnectionFailureException.class);

        verify(inventoryRepository).findReservations(42);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void listsDistinctProductStockAndOmitsUnknownOnes() {
        when(inventoryRepository.findInventories(List.of(101L, 102L)))
                .thenReturn(List.of(new Inventory(101, 10, 7, 2, 1, 3, Instant.EPOCH)));

        var result = inventoryService.listInventory(List.of(101L, 101L, 102L));

        assertThat(result).singleElement().satisfies(view -> {
            assertThat(view.productId()).isEqualTo(101);
            assertThat(view.availableQuantity()).isEqualTo(7);
        });
    }

    @Test
    void commitsCancelIntentToMysqlWithoutCallingRedisInline() {
        var reservation = new InventoryReservation(
                42,
                USER_ID,
                101,
                2,
                ReservationStatus.TRY,
                Instant.now().plusSeconds(60),
                Instant.EPOCH,
                Instant.EPOCH);
        when(inventoryRepository.findReservations(42))
                .thenReturn(List.of(reservation))
                .thenReturn(List.of(new InventoryReservation(
                        42,
                        USER_ID,
                        101,
                        2,
                        ReservationStatus.CANCELLED,
                        reservation.expiresAt(),
                        Instant.EPOCH,
                        Instant.EPOCH)));
        when(inventoryRepository.markCancelled(42, 101)).thenReturn(1);
        when(inventoryRepository.cancelStock(101, 2)).thenReturn(1);

        inventoryService.cancelAll(42);

        verify(inventoryRepository).enqueueInventorySync(42, 101, InventorySyncOperation.CANCEL);
        verifyNoInteractions(reservationStore);
    }

    @Test
    void commitsConfirmIntentToMysqlWithoutCallingRedisInline() {
        var reservation = new InventoryReservation(
                43,
                USER_ID,
                102,
                2,
                ReservationStatus.TRY,
                Instant.now().plusSeconds(60),
                Instant.EPOCH,
                Instant.EPOCH);
        when(inventoryRepository.findReservations(43))
                .thenReturn(List.of(reservation))
                .thenReturn(List.of(new InventoryReservation(
                        43,
                        USER_ID,
                        102,
                        2,
                        ReservationStatus.CONFIRMED,
                        reservation.expiresAt(),
                        Instant.EPOCH,
                        Instant.EPOCH)));
        when(inventoryRepository.markConfirmed(43, 102)).thenReturn(1);
        when(inventoryRepository.confirmStock(102, 2)).thenReturn(1);

        inventoryService.confirmAll(43);

        verify(inventoryRepository).enqueueInventorySync(43, 102, InventorySyncOperation.CONFIRM);
        verifyNoInteractions(reservationStore);
    }

    @Test
    void keepsFailedRedisSynchronizationPendingForRetry() {
        InventorySyncTask task = new InventorySyncTask(9, 42, 101, InventorySyncOperation.CONFIRM);
        when(inventoryRepository.findPendingInventorySync(10)).thenReturn(List.of(task));
        doThrow(new RedisConnectionFailureException("Redis unavailable"))
                .when(reservationStore)
                .confirm(101, 42, Duration.ofHours(24));

        assertThat(inventoryService.synchronizeRedis(10)).isZero();

        verify(inventoryRepository).markInventorySyncFailed(9, "Redis unavailable");
    }
}
