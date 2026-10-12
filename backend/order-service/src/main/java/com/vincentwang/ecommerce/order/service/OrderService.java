package com.vincentwang.ecommerce.order.service;

import com.vincentwang.ecommerce.common.pagination.PageResult;
import com.vincentwang.ecommerce.common.pagination.Pagination;
import com.vincentwang.ecommerce.order.client.catalog.IProductCatalogClient;
import com.vincentwang.ecommerce.order.client.inventory.IInventoryReservationClient;
import com.vincentwang.ecommerce.order.controller.dto.request.CreateOrderItemRequest;
import com.vincentwang.ecommerce.order.controller.dto.request.CreateOrderRequest;
import com.vincentwang.ecommerce.order.controller.dto.response.OrderResponse;
import com.vincentwang.ecommerce.order.id.IOrderIdGenerator;
import com.vincentwang.ecommerce.order.messaging.IEventPayloadSerializer;
import com.vincentwang.ecommerce.order.messaging.OrderEvents;
import com.vincentwang.ecommerce.order.model.InventoryReservationItem;
import com.vincentwang.ecommerce.order.model.Order;
import com.vincentwang.ecommerce.order.model.OrderItem;
import com.vincentwang.ecommerce.order.model.OrderStatus;
import com.vincentwang.ecommerce.order.model.ProductQuote;
import com.vincentwang.ecommerce.order.repository.IOrderRepository;
import com.vincentwang.ecommerce.order.service.exception.OrderConflictException;
import com.vincentwang.ecommerce.order.service.exception.OrderNotFoundException;
import com.vincentwang.ecommerce.order.service.exception.ProductUnavailableException;
import com.vincentwang.ecommerce.order.service.exception.SelfPurchaseNotAllowedException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order lifecycle: ACCEPTED, then PENDING_PAYMENT, then CONFIRMING_INVENTORY, and finally PAID or an explicit
 * recovery state. Unpaid orders become EXPIRED.
 * See {@link OrderEvents} for the events that connect these steps.
 */
@Service
public class OrderService implements IOrderService {
    private static final String WORKFLOW_CONSUMER = "order-workflow-v1";
    private static final String INVENTORY_CONSUMER = "inventory-command-v1";

    private final IOrderRepository orderRepository;
    private final IProductCatalogClient catalogClient;
    private final IInventoryReservationClient inventoryClient;
    private final IOrderIdGenerator orderIdGenerator;
    private final IEventPayloadSerializer eventSerializer;
    private final Clock clock = Clock.systemUTC();
    private final Duration paymentDeadline;
    private final long reservationTtlSeconds;

    public OrderService(
            IOrderRepository orderRepository,
            IProductCatalogClient catalogClient,
            IInventoryReservationClient inventoryClient,
            IOrderIdGenerator orderIdGenerator,
            IEventPayloadSerializer eventSerializer,
            @Value("${order.payment-deadline}") Duration paymentDeadline,
            @Value("${order.inventory.reservation-ttl-seconds}") long reservationTtlSeconds) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.orderIdGenerator = orderIdGenerator;
        this.eventSerializer = eventSerializer;
        this.paymentDeadline = paymentDeadline;
        this.reservationTtlSeconds = reservationTtlSeconds;
    }

    /**
     * Accepts an order: price it from the catalog, hold the stock, then persist the order and its workflow
     * event together. A repeated idempotency key returns the original order without touching either service.
     */
    @Transactional
    @Override
    public OrderResponse submit(String userId, CreateOrderRequest request, String idempotencyKey) {
        String key = idempotencyKey.trim();
        Order existing = orderRepository.findByIdempotencyKey(userId, key).orElse(null);
        if (existing != null) {
            return OrderResponse.from(hydrate(existing));
        }

        List<CreateOrderItemRequest> requestedItems = normalizeItems(request.items());
        List<PricedItem> pricedItems = requestedItems.stream().map(this::price).toList();
        pricedItems.stream()
                .filter(item -> userId.equals(item.quote().userId()))
                .findFirst()
                .ifPresent(item -> {
                    throw new SelfPurchaseNotAllowedException(item.request().productId());
                });
        String currency = pricedItems.getFirst().quote().currency();
        if (pricedItems.stream().anyMatch(item -> !currency.equals(item.quote().currency()))) {
            throw new OrderConflictException("All items in an order must use the same currency");
        }
        long totalMinor = total(pricedItems);

        long orderId = orderIdGenerator.nextId();
        inventoryClient.tryReserveAll(
                orderId,
                userId,
                requestedItems.stream()
                        .map(item -> new InventoryReservationItem(item.productId(), item.quantity()))
                        .toList(),
                reservationTtlSeconds);

        LocalDateTime now = now();
        try {
            orderRepository.insertOrder(new Order(
                    orderId,
                    userId,
                    OrderStatus.ACCEPTED,
                    totalMinor,
                    currency,
                    key,
                    now.plus(paymentDeadline),
                    0,
                    null,
                    null,
                    List.of()));
            for (PricedItem item : pricedItems) {
                orderRepository.insertItem(new OrderItem(
                        orderIdGenerator.nextId(),
                        orderId,
                        userId,
                        item.request().productId(),
                        item.quote().name(),
                        item.quote().unitPriceMinor(),
                        item.request().quantity()));
            }
            writeOutbox(
                    userId,
                    orderId,
                    OrderEvents.ORDER_REQUESTED,
                    Long.toString(orderId),
                    new OrderEvents.OrderRequested(
                            orderId,
                            userId,
                            requestedItems.stream()
                                    .map(item -> item.productId())
                                    .toList()),
                    now);
        } catch (DuplicateKeyException exception) {
            // A concurrent request with the same key won the race; give back our hold and return its order.
            releaseHold(orderId, exception);
            return orderRepository
                    .findByIdempotencyKey(userId, key)
                    .map(this::hydrate)
                    .map(OrderResponse::from)
                    .orElseThrow(() -> exception);
        } catch (RuntimeException exception) {
            releaseHold(orderId, exception);
            throw exception;
        }
        return get(userId, orderId);
    }

    @Transactional(readOnly = true)
    @Override
    public OrderResponse get(String userId, long orderId) {
        return OrderResponse.from(hydrate(
                orderRepository.findById(userId, orderId).orElseThrow(() -> new OrderNotFoundException(orderId))));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResult<OrderResponse> list(String userId, int page, int size) {
        long offset = Pagination.offset(page, size);
        List<OrderResponse> orders = orderRepository.findByUserId(userId, offset, size).stream()
                .map(this::hydrate)
                .map(OrderResponse::from)
                .toList();
        return new PageResult<>(orders, page, size, orderRepository.countByUserId(userId));
    }

    @Transactional
    @Override
    public OrderResponse pay(String userId, long orderId, String idempotencyKey) {
        Order order = orderRepository.findById(userId, orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.CONFIRMING_INVENTORY
                || order.getStatus() == OrderStatus.INVENTORY_CONFIRMATION_FAILED) {
            return OrderResponse.from(hydrate(order));
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new OrderConflictException("Only a pending-payment order can be paid");
        }
        if (orderRepository.markConfirmingInventory(userId, orderId) != 1) {
            throw new OrderConflictException("Order status changed while payment was processed");
        }
        writeOutbox(
                userId,
                orderId,
                OrderEvents.INVENTORY_CONFIRM,
                Long.toString(orderId),
                new OrderEvents.InventoryCommand(orderId, order.getUserId(), idempotencyKey),
                now());
        return get(userId, orderId);
    }

    @Transactional
    @Override
    public void activate(String eventId, String userId, long orderId) {
        if (orderRepository.insertProcessedEvent(userId, WORKFLOW_CONSUMER, eventId) == 0) {
            return;
        }
        Order order = orderRepository.findById(userId, orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getStatus() == OrderStatus.ACCEPTED) {
            orderRepository.markPendingPayment(userId, orderId);
        }
    }

    @Transactional
    @Override
    public void applyInventoryCommand(String eventId, String eventType, String userId, long orderId) {
        if (!OrderEvents.isInventoryCommand(eventType)) {
            throw new IllegalArgumentException("Unsupported inventory command: " + eventType);
        }
        if (orderRepository.insertProcessedEvent(userId, INVENTORY_CONSUMER, eventId) == 0) {
            return;
        }
        if (OrderEvents.INVENTORY_CONFIRM.equals(eventType)) {
            inventoryClient.confirm(orderId);
            if (orderRepository.markPaid(userId, orderId) != 1) {
                Order order = orderRepository
                        .findById(userId, orderId)
                        .orElseThrow(() -> new OrderNotFoundException(orderId));
                if (order.getStatus() != OrderStatus.PAID) {
                    throw new OrderConflictException("Order cannot complete inventory confirmation");
                }
            }
        } else {
            inventoryClient.cancel(orderId);
        }
    }

    @Transactional
    @Override
    public void markInventoryConfirmationFailed(String userId, long orderId) {
        orderRepository.markInventoryConfirmationFailed(userId, orderId);
    }

    @Transactional
    @Override
    public int expireBatch(int batchSize) {
        LocalDateTime now = now();
        int expired = 0;
        for (Order order : orderRepository.findExpiredForUpdate(batchSize)) {
            if (orderRepository.markExpired(order.getUserId(), order.getOrderId()) == 1) {
                writeOutbox(
                        order.getUserId(),
                        order.getOrderId(),
                        OrderEvents.INVENTORY_CANCEL,
                        Long.toString(order.getOrderId()),
                        new OrderEvents.InventoryCommand(order.getOrderId(), order.getUserId(), "expiry"),
                        now);
                expired++;
            }
        }
        return expired;
    }

    private ProductQuote requirePurchasable(long productId) {
        ProductQuote quote = catalogClient.quote(productId);
        if (!quote.purchasable()) {
            throw new ProductUnavailableException(productId);
        }
        return quote;
    }

    private PricedItem price(CreateOrderItemRequest request) {
        return new PricedItem(request, requirePurchasable(request.productId()));
    }

    private static long total(List<PricedItem> items) {
        try {
            long total = 0;
            for (PricedItem item : items) {
                total = Math.addExact(
                        total,
                        Math.multiplyExact(
                                item.quote().unitPriceMinor(), item.request().quantity()));
            }
            return total;
        } catch (ArithmeticException exception) {
            throw new OrderConflictException("Order total exceeds the supported range");
        }
    }

    private static List<CreateOrderItemRequest> normalizeItems(List<CreateOrderItemRequest> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) {
            throw new OrderConflictException("At least one order item is required");
        }
        Map<Long, Long> quantities = new LinkedHashMap<>();
        for (CreateOrderItemRequest item : rawItems) {
            long productId = item.productId();
            if (productId <= 0) {
                throw new OrderConflictException("Product ID must be positive");
            }
            if (item.quantity() <= 0) {
                throw new OrderConflictException("Order quantity must be positive");
            }
            try {
                Long existingQuantity = quantities.get(productId);
                long mergedQuantity = existingQuantity == null
                        ? item.quantity()
                        : Math.addExact(existingQuantity.longValue(), item.quantity());
                quantities.put(productId, mergedQuantity);
            } catch (ArithmeticException exception) {
                throw new OrderConflictException("Order quantity exceeds the supported range");
            }
        }
        return quantities.entrySet().stream()
                .map(entry -> new CreateOrderItemRequest(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** Best-effort compensation: a failed release must not hide the error that made us release. */
    private void releaseHold(long orderId, RuntimeException cause) {
        try {
            inventoryClient.cancel(orderId);
        } catch (RuntimeException releaseFailure) {
            cause.addSuppressed(releaseFailure);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private Order hydrate(Order order) {
        order.setItems(orderRepository.findItems(order.getUserId(), order.getOrderId()));
        return order;
    }

    private void writeOutbox(
            String userId, long orderId, String eventType, String partitionKey, Object payload, LocalDateTime now) {
        orderRepository.appendOutboxEvent(
                userId,
                UUID.randomUUID().toString(),
                Long.toString(orderId),
                eventType,
                partitionKey,
                eventSerializer.serialize(payload),
                now);
    }

    private record PricedItem(CreateOrderItemRequest request, ProductQuote quote) {}
}
