package com.vincentwang.ecommerce.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincentwang.ecommerce.inventory.controller.dto.request.CreateInventoryRequest;
import com.vincentwang.ecommerce.inventory.controller.dto.request.ReservationItemRequest;
import com.vincentwang.ecommerce.inventory.controller.dto.request.TryReservationRequest;
import com.vincentwang.ecommerce.inventory.controller.dto.request.TryReservationsRequest;
import com.vincentwang.ecommerce.inventory.controller.dto.response.InventoryResponse;
import com.vincentwang.ecommerce.inventory.controller.dto.response.ReservationResponse;
import com.vincentwang.ecommerce.inventory.model.ReservationStatus;
import com.vincentwang.ecommerce.inventory.service.IInventoryService;
import com.vincentwang.ecommerce.inventory.service.exception.InsufficientStockException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "inventory.reservation.expiry-delay=1h",
            "inventory.reservation.expiry-batch-size=100",
            "inventory.redis-sync.initial-delay=1h",
            "INVENTORY_DRIFT_CHECK_INITIAL_DELAY=1h"
        })
class InventoryReservationIntegrationTest {

    private static final String USER_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
    private static final String OTHER_USER_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaab";

    @SuppressWarnings("resource")
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.11")
            .withDatabaseName("inventory_db")
            .withUsername("ecommerce")
            .withPassword("test-password");

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.2.10").withExposedPorts(6379);

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IInventoryService inventoryService;

    private RestClient client;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM inventory_reservation");
        jdbcTemplate.update("DELETE FROM inventory_redis_sync");
        jdbcTemplate.update("DELETE FROM inventory");
        try (RedisConnection connection =
                Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()) {
            connection.serverCommands().flushAll();
        }
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    void performsIdempotentTryConfirmAndCancelTransitions() {
        InventoryResponse createdInventory = createInventory(101, 10);
        assertThat(createInventory(101, 10)).isEqualTo(createdInventory);

        TryReservationRequest request = new TryReservationRequest(1001, USER_ID, 101, 2, 60L);
        ReservationResponse reserved = client.post()
                .uri("/api/v1/inventory/reservations/try")
                .body(request)
                .retrieve()
                .body(ReservationResponse.class);
        ReservationResponse duplicateTry = client.post()
                .uri("/api/v1/inventory/reservations/try")
                .body(request)
                .retrieve()
                .body(ReservationResponse.class);

        assertThat(reserved).isNotNull();
        assertThat(duplicateTry).isEqualTo(reserved);
        assertThat(reserved.status()).isEqualTo(ReservationStatus.TRY);

        ReservationResponse confirmed = transition(1001, "confirm");
        assertThat(transition(1001, "confirm")).isEqualTo(confirmed);
        assertThat(confirmed.status()).isEqualTo(ReservationStatus.CONFIRMED);

        createInventory(102, 5);
        reserve(1002, OTHER_USER_ID, 102, 3, 60);
        ReservationResponse cancelled = transition(1002, "cancel");
        assertThat(transition(1002, "cancel")).isEqualTo(cancelled);
        assertThat(cancelled.status()).isEqualTo(ReservationStatus.CANCELLED);

        InventoryResponse first = getInventory(101);
        assertThat(first.availableQuantity()).isEqualTo(8);
        assertThat(first.reservedQuantity()).isZero();
        assertThat(first.soldQuantity()).isEqualTo(2);

        InventoryResponse second = getInventory(102);
        assertThat(second.availableQuantity()).isEqualTo(5);
        assertThat(second.reservedQuantity()).isZero();
        assertThat(second.soldQuantity()).isZero();
    }

    @Test
    void expiresTryReservationsAndRestoresStock() throws InterruptedException {
        createInventory(201, 4);
        createInventory(202, 6);
        var request = new TryReservationsRequest(
                2001, USER_ID, List.of(new ReservationItemRequest(201, 3), new ReservationItemRequest(202, 2)), 1L);
        client.post()
                .uri("/api/v1/inventory/reservations/try-all")
                .body(request)
                .retrieve()
                .toBodilessEntity();

        Thread.sleep(Duration.ofSeconds(1).plusMillis(100));
        assertThat(inventoryService.cancelExpiredReservations(100)).isEqualTo(1);
        var reservations = client.get()
                .uri("/api/v1/inventory/reservations/2001/all")
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<ReservationResponse>>() {});

        assertThat(reservations).hasSize(2).allMatch(item -> item.status() == ReservationStatus.CANCELLED);
        InventoryResponse first = getInventory(201);
        assertThat(first.availableQuantity()).isEqualTo(4);
        assertThat(first.reservedQuantity()).isZero();
        InventoryResponse second = getInventory(202);
        assertThat(second.availableQuantity()).isEqualTo(6);
        assertThat(second.reservedQuantity()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM inventory_redis_sync WHERE order_id = 2001 AND operation = 'CANCEL'",
                        Integer.class))
                .isEqualTo(2);
    }

    @Test
    void reservesAndConfirmsMultipleProductsAtomically() {
        createInventory(301, 10);
        createInventory(302, 8);
        var request = new TryReservationsRequest(
                4001, USER_ID, List.of(new ReservationItemRequest(301, 2), new ReservationItemRequest(302, 3)), 60L);

        var reserved = client.post()
                .uri("/api/v1/inventory/reservations/try-all")
                .body(request)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<ReservationResponse>>() {});
        var confirmed = client.post()
                .uri("/api/v1/inventory/reservations/4001/confirm-all")
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<ReservationResponse>>() {});

        assertThat(reserved).hasSize(2).allMatch(item -> item.status() == ReservationStatus.TRY);
        assertThat(confirmed).hasSize(2).allMatch(item -> item.status() == ReservationStatus.CONFIRMED);
        assertThat(getInventory(301).soldQuantity()).isEqualTo(2);
        assertThat(getInventory(302).soldQuantity()).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM inventory_redis_sync WHERE order_id = 4001 AND applied_at IS NULL",
                        Integer.class))
                .isEqualTo(2);

        assertThat(inventoryService.synchronizeRedis(10)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM inventory_redis_sync WHERE order_id = 4001 AND applied_at IS NULL",
                        Integer.class))
                .isZero();
    }

    @Test
    void preventsOversellingAcrossOneThousandConcurrentAttempts() throws Exception {
        inventoryService.createInventory(401, new CreateInventoryRequest(100));
        AtomicInteger reserved = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ConcurrentLinkedQueue<Long> successfulOrderIds = new ConcurrentLinkedQueue<>();

        try (var executor = Executors.newFixedThreadPool(32)) {
            var attempts = IntStream.rangeClosed(1, 1_000)
                    .mapToObj(number -> executor.submit(() -> {
                        try {
                            inventoryService.tryReserve(
                                    new TryReservationRequest(number, testUserId(number), 401, 1, 60L));
                            reserved.incrementAndGet();
                            successfulOrderIds.add((long) number);
                        } catch (InsufficientStockException exception) {
                            rejected.incrementAndGet();
                        }
                    }))
                    .toList();
            for (var attempt : attempts) {
                attempt.get();
            }
        }

        assertThat(reserved).hasValue(100);
        assertThat(rejected).hasValue(900);

        var winners = successfulOrderIds.stream().sorted().toList();
        for (int index = 0; index < 50; index++) {
            inventoryService.confirm(Objects.requireNonNull(winners.get(index)).longValue());
        }
        for (int index = 50; index < 100; index++) {
            inventoryService.cancel(Objects.requireNonNull(winners.get(index)).longValue());
        }

        InventoryResponse inventory = inventoryService.getInventory(401);
        assertThat(inventory.totalQuantity()).isEqualTo(100);
        assertThat(inventory.availableQuantity()).isEqualTo(50);
        assertThat(inventory.reservedQuantity()).isZero();
        assertThat(inventory.soldQuantity()).isEqualTo(50);
        assertThat(inventory.availableQuantity() + inventory.reservedQuantity() + inventory.soldQuantity())
                .isEqualTo(inventory.totalQuantity());
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM inventory_reservation WHERE status = 'TRY'", Integer.class))
                .isZero();
    }

    @Test
    void rejectsRequestsAfterStockIsExhausted() {
        createInventory(501, 1);
        reserve(3001, USER_ID, 501, 1, 60);

        HttpStatusCode status = client.post()
                .uri("/api/v1/inventory/reservations/try")
                .body(new TryReservationRequest(3002, OTHER_USER_ID, 501, 1, 60L))
                .exchange((request, response) -> response.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.CONFLICT);
    }

    private InventoryResponse createInventory(long productId, long quantity) {
        return client.put()
                .uri("/api/v1/inventory/{productId}", productId)
                .body(new CreateInventoryRequest(quantity))
                .retrieve()
                .body(InventoryResponse.class);
    }

    private ReservationResponse reserve(long orderId, String userId, long productId, long quantity, long ttlSeconds) {
        return client.post()
                .uri("/api/v1/inventory/reservations/try")
                .body(new TryReservationRequest(orderId, userId, productId, quantity, ttlSeconds))
                .retrieve()
                .body(ReservationResponse.class);
    }

    private ReservationResponse transition(long orderId, String transition) {
        return client.post()
                .uri("/api/v1/inventory/reservations/{orderId}/{transition}", orderId, transition)
                .retrieve()
                .body(ReservationResponse.class);
    }

    private InventoryResponse getInventory(long productId) {
        return client.get()
                .uri("/api/v1/inventory/{productId}", productId)
                .retrieve()
                .body(InventoryResponse.class);
    }

    private static String testUserId(int number) {
        return "00000000-0000-4000-8000-" + String.format("%012d", number);
    }
}
