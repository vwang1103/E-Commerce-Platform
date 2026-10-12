package com.vincentwang.ecommerce.inventory.application;

import com.vincentwang.ecommerce.inventory.api.dto.request.CreateInventoryRequest;
import com.vincentwang.ecommerce.inventory.api.dto.request.ReservationItemRequest;
import com.vincentwang.ecommerce.inventory.api.dto.request.TryReservationRequest;
import com.vincentwang.ecommerce.inventory.api.dto.request.TryReservationsRequest;
import com.vincentwang.ecommerce.inventory.api.dto.response.InventoryResponse;
import com.vincentwang.ecommerce.inventory.api.dto.response.ReservationResponse;
import com.vincentwang.ecommerce.inventory.application.exception.InsufficientStockException;
import com.vincentwang.ecommerce.inventory.application.exception.InventoryConflictException;
import com.vincentwang.ecommerce.inventory.application.exception.InventoryNotFoundException;
import com.vincentwang.ecommerce.inventory.application.exception.ReservationNotFoundException;
import com.vincentwang.ecommerce.inventory.application.model.InventorySyncOperation;
import com.vincentwang.ecommerce.inventory.application.model.InventorySyncTask;
import com.vincentwang.ecommerce.inventory.application.repository.IInventoryRepository;
import com.vincentwang.ecommerce.inventory.domain.model.Inventory;
import com.vincentwang.ecommerce.inventory.domain.model.InventoryReservation;
import com.vincentwang.ecommerce.inventory.domain.model.ReservationStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryApplicationService implements IInventoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryApplicationService.class);

    private final IInventoryRepository inventoryRepository;
    private final IInventoryReservationStore reservationStore;
    private final Clock clock;
    private final Duration defaultTtl;
    private final Duration maximumTtl;
    private final Duration redisRetention;

    public InventoryApplicationService(
            IInventoryRepository inventoryRepository,
            IInventoryReservationStore reservationStore,
            @Value("${inventory.reservation.default-ttl}") Duration defaultTtl,
            @Value("${inventory.reservation.maximum-ttl}") Duration maximumTtl,
            @Value("${inventory.reservation.redis-retention}") Duration redisRetention) {
        this.inventoryRepository = inventoryRepository;
        this.reservationStore = reservationStore;
        this.clock = Clock.systemUTC();
        this.defaultTtl = defaultTtl;
        this.maximumTtl = maximumTtl;
        this.redisRetention = redisRetention;
    }

    @Transactional
    @Override
    public InventoryResponse createInventory(long productId, CreateInventoryRequest request) {
        long totalQuantity = request.totalQuantity();
        Inventory existing = inventoryRepository.findInventory(productId);
        if (existing != null) {
            if (existing.totalQuantity() != totalQuantity) {
                throw new InventoryConflictException(
                        "Inventory already exists with a different total for product " + productId);
            }
            return InventoryResponse.from(existing);
        }
        try {
            inventoryRepository.insertInventory(productId, totalQuantity);
        } catch (DuplicateKeyException exception) {
            Inventory concurrent = requireInventory(productId);
            if (concurrent.totalQuantity() != totalQuantity) {
                throw new InventoryConflictException(
                        "Inventory already exists with a different total for product " + productId);
            }
            return InventoryResponse.from(concurrent);
        }
        reservationStore.initialize(productId, totalQuantity);
        return InventoryResponse.from(requireInventory(productId));
    }

    @Transactional(readOnly = true)
    @Override
    public InventoryResponse getInventory(long productId) {
        return InventoryResponse.from(requireInventory(productId));
    }

    @Transactional(readOnly = true)
    @Override
    public List<InventoryResponse> listInventory(Collection<Long> productIds) {
        return inventoryRepository
                .findInventories(productIds.stream().distinct().toList())
                .stream()
                .map(InventoryResponse::from)
                .toList();
    }

    @Transactional
    @Override
    public ReservationResponse tryReserve(TryReservationRequest request) {
        return tryReserveAll(new TryReservationsRequest(
                        request.orderId(),
                        request.userId(),
                        List.of(new ReservationItemRequest(request.productId(), request.quantity())),
                        request.ttlSeconds()))
                .getFirst();
    }

    @Transactional
    @Override
    public List<ReservationResponse> tryReserveAll(TryReservationsRequest request) {
        long orderId = request.orderId();
        String userId = request.userId();
        Long requestedTtlSeconds = request.ttlSeconds();
        Duration ttl = requestedTtlSeconds == null ? defaultTtl : Duration.ofSeconds(requestedTtlSeconds);
        if (ttl.compareTo(maximumTtl) > 0) {
            throw new InventoryConflictException("Reservation TTL exceeds the configured maximum");
        }
        List<ReservationItemRequest> items = normalizeItems(request.items());
        List<InventoryReservation> durableReservations = inventoryRepository.findReservations(orderId);
        if (!durableReservations.isEmpty()) {
            validateExistingReservations(userId, items, durableReservations);
            return durableReservations.stream().map(ReservationResponse::from).toList();
        }
        Instant expiresAt = clock.instant().plus(ttl);
        List<ReservationItemRequest> newRedisHolds = new java.util.ArrayList<>();
        try {
            for (ReservationItemRequest item : items) {
                long code = reservationStore.tryReserve(
                        item.productId(), orderId, userId, item.quantity(), expiresAt, ttl.plus(redisRetention));
                switch ((int) code) {
                    case -1 -> throw new InventoryNotFoundException(item.productId());
                    case -2 -> throw new InsufficientStockException(item.productId());
                    case -3 ->
                        throw new InventoryConflictException("Order ID is already used by a different reservation");
                    case -4 -> throw new InventoryConflictException("A cancelled reservation cannot be reserved again");
                    default -> {
                        if (code == 1) {
                            newRedisHolds.add(item);
                        }
                    }
                }
            }
            for (ReservationItemRequest item : items) {
                persistTry(orderId, userId, item.productId(), item.quantity(), expiresAt);
            }
        } catch (RuntimeException exception) {
            compensateRedisHolds(orderId, newRedisHolds, exception);
            throw exception;
        }
        return inventoryRepository.findReservations(orderId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    @Override
    public ReservationResponse confirm(long orderId) {
        return confirmAll(orderId).getFirst();
    }

    @Transactional
    @Override
    public List<ReservationResponse> confirmAll(long orderId) {
        List<InventoryReservation> reservations = requireReservations(orderId);
        for (InventoryReservation reservation : reservations) {
            if (reservation.status() == ReservationStatus.CANCELLED) {
                throw new InventoryConflictException("Cancelled reservation cannot be confirmed");
            }
            if (reservation.status() == ReservationStatus.TRY) {
                if (inventoryRepository.markConfirmed(orderId, reservation.productId()) == 1
                        && inventoryRepository.confirmStock(reservation.productId(), reservation.quantity()) != 1) {
                    throw new InventoryConflictException("Durable reserved stock is inconsistent for order " + orderId);
                }
                inventoryRepository.enqueueInventorySync(
                        orderId, reservation.productId(), InventorySyncOperation.CONFIRM);
            }
        }
        return getReservations(orderId);
    }

    @Transactional
    @Override
    public ReservationResponse cancel(long orderId) {
        return cancelAll(orderId).getFirst();
    }

    @Transactional
    @Override
    public List<ReservationResponse> cancelAll(long orderId) {
        List<InventoryReservation> reservations = requireReservations(orderId);
        for (InventoryReservation reservation : reservations) {
            if (reservation.status() == ReservationStatus.CONFIRMED) {
                throw new InventoryConflictException("Confirmed reservation cannot be cancelled");
            }
            if (reservation.status() == ReservationStatus.TRY) {
                if (inventoryRepository.markCancelled(orderId, reservation.productId()) == 1
                        && inventoryRepository.cancelStock(reservation.productId(), reservation.quantity()) != 1) {
                    throw new InventoryConflictException("Durable reserved stock is inconsistent for order " + orderId);
                }
                inventoryRepository.enqueueInventorySync(
                        orderId, reservation.productId(), InventorySyncOperation.CANCEL);
            }
        }
        return getReservations(orderId);
    }

    @Transactional(readOnly = true)
    @Override
    public ReservationResponse getReservation(long orderId) {
        return ReservationResponse.from(requireReservation(orderId));
    }

    @Transactional(readOnly = true)
    @Override
    public List<ReservationResponse> getReservations(long orderId) {
        return requireReservations(orderId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    @Override
    public int synchronizeRedis(int batchSize) {
        int synchronizedCount = 0;
        for (InventorySyncTask task : inventoryRepository.findPendingInventorySync(batchSize)) {
            try {
                long code = task.operation() == InventorySyncOperation.CONFIRM
                        ? reservationStore.confirm(task.productId(), task.orderId(), redisRetention)
                        : reservationStore.cancel(task.productId(), task.orderId(), redisRetention);
                if (code != 1 && code != 2) {
                    throw new IllegalStateException("Redis reservation transition returned code " + code);
                }
                inventoryRepository.markInventorySyncApplied(task.syncId());
                synchronizedCount++;
            } catch (RuntimeException exception) {
                inventoryRepository.markInventorySyncFailed(task.syncId(), abbreviate(exception.getMessage()));
                LOGGER.warn(
                        "Redis inventory synchronization failed for order {} product {}",
                        task.orderId(),
                        task.productId(),
                        exception);
            }
        }
        return synchronizedCount;
    }

    @Transactional
    @Override
    public int cancelExpiredReservations(int batchSize) {
        int cancelled = 0;
        for (long orderId : inventoryRepository.findExpiredTryReservations(clock.instant(), batchSize).stream()
                .map(reservation -> reservation.orderId())
                .distinct()
                .toList()) {
            cancelAll(orderId);
            cancelled++;
        }
        return cancelled;
    }

    @Override
    public void detectDrift() {
        for (Inventory inventory : inventoryRepository.findAllInventory()) {
            Long redisAvailable = reservationStore.available(inventory.productId());
            if (redisAvailable == null || redisAvailable != inventory.availableQuantity()) {
                LOGGER.error(
                        "Inventory drift detected for product {}: mysqlAvailable={}, redisAvailable={}",
                        inventory.productId(),
                        inventory.availableQuantity(),
                        redisAvailable);
            }
            long accounted = inventory.availableQuantity() + inventory.reservedQuantity() + inventory.soldQuantity();
            if (accounted != inventory.totalQuantity()) {
                LOGGER.error(
                        "Inventory conservation violation for product {}: total={}, accounted={}",
                        inventory.productId(),
                        inventory.totalQuantity(),
                        accounted);
            }
        }
    }

    private InventoryReservation persistTry(
            long orderId, String userId, long productId, long quantity, Instant expiresAt) {
        InventoryReservation existing = inventoryRepository.findReservations(orderId).stream()
                .filter(reservation -> reservation.productId() == productId)
                .findFirst()
                .orElse(null);
        if (existing != null) {
            if (!existing.userId().equals(userId)
                    || existing.productId() != productId
                    || existing.quantity() != quantity) {
                throw new InventoryConflictException("Order ID is already used by a different reservation");
            }
            return existing;
        }
        if (inventoryRepository.reserveStock(productId, quantity) != 1) {
            if (inventoryRepository.findInventory(productId) == null) {
                throw new InventoryNotFoundException(productId);
            }
            throw new InsufficientStockException(productId);
        }
        InventoryReservation reservation = new InventoryReservation(
                orderId, userId, productId, quantity, ReservationStatus.TRY, expiresAt, null, null);
        inventoryRepository.insertReservation(reservation);
        return requireReservation(orderId);
    }

    private Inventory requireInventory(long productId) {
        Inventory inventory = inventoryRepository.findInventory(productId);
        if (inventory == null) {
            throw new InventoryNotFoundException(productId);
        }
        return inventory;
    }

    private InventoryReservation requireReservation(long orderId) {
        InventoryReservation reservation = inventoryRepository.findReservation(orderId);
        if (reservation == null) {
            throw new ReservationNotFoundException(orderId);
        }
        return reservation;
    }

    private List<InventoryReservation> requireReservations(long orderId) {
        List<InventoryReservation> reservations = inventoryRepository.findReservations(orderId);
        if (reservations.isEmpty()) {
            throw new ReservationNotFoundException(orderId);
        }
        return reservations;
    }

    private static List<ReservationItemRequest> normalizeItems(List<ReservationItemRequest> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) {
            throw new InventoryConflictException("At least one reservation item is required");
        }
        Map<Long, Long> quantities = new LinkedHashMap<>();
        for (ReservationItemRequest item : rawItems) {
            long productId = item.productId();
            if (productId <= 0) {
                throw new InventoryConflictException("Product ID must be positive");
            }
            if (item.quantity() <= 0) {
                throw new InventoryConflictException("Reservation quantity must be positive");
            }
            try {
                Long existingQuantity = quantities.get(productId);
                long mergedQuantity = existingQuantity == null
                        ? item.quantity()
                        : Math.addExact(existingQuantity.longValue(), item.quantity());
                quantities.put(productId, mergedQuantity);
            } catch (ArithmeticException exception) {
                throw new InventoryConflictException("Reservation quantity exceeds the supported range");
            }
        }
        return quantities.entrySet().stream()
                .map(entry -> new ReservationItemRequest(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static void validateExistingReservations(
            String userId, List<ReservationItemRequest> items, List<InventoryReservation> reservations) {
        Map<Long, Long> expected = new LinkedHashMap<>();
        items.forEach(item -> expected.put(item.productId(), item.quantity()));
        Map<Long, Long> actual = new LinkedHashMap<>();
        for (InventoryReservation reservation : reservations) {
            if (!reservation.userId().equals(userId) || reservation.status() == ReservationStatus.CANCELLED) {
                throw new InventoryConflictException("Order ID is already used by a different reservation");
            }
            actual.put(reservation.productId(), reservation.quantity());
        }
        if (!actual.equals(expected)) {
            throw new InventoryConflictException("Order ID is already used by a different reservation");
        }
    }

    private void compensateRedisHolds(
            long orderId, List<ReservationItemRequest> newRedisHolds, RuntimeException originalFailure) {
        for (ReservationItemRequest item : newRedisHolds.reversed()) {
            try {
                reservationStore.cancel(item.productId(), orderId, redisRetention);
            } catch (RuntimeException compensationFailure) {
                originalFailure.addSuppressed(compensationFailure);
            }
        }
    }

    private static String abbreviate(String message) {
        String value = message == null ? "Unknown Redis synchronization failure" : message;
        return value.substring(0, Math.min(value.length(), 500));
    }
}
