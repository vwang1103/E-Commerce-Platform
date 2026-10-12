package com.vincentwang.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vincentwang.ecommerce.common.web.PlatformHttpHeaders;
import com.vincentwang.ecommerce.order.client.catalog.ICatalogFeignClient;
import com.vincentwang.ecommerce.order.client.catalog.dto.FeignProductResponse;
import com.vincentwang.ecommerce.order.client.inventory.IInventoryFeignClient;
import com.vincentwang.ecommerce.order.client.inventory.dto.FeignInventoryReservationItem;
import com.vincentwang.ecommerce.order.client.inventory.dto.FeignInventoryReservationResponse;
import com.vincentwang.ecommerce.order.client.inventory.dto.FeignInventoryTryRequest;
import com.vincentwang.ecommerce.order.controller.dto.request.CreateOrderItemRequest;
import com.vincentwang.ecommerce.order.controller.dto.request.CreateOrderRequest;
import com.vincentwang.ecommerce.order.controller.dto.response.OrderResponse;
import com.vincentwang.ecommerce.order.model.OrderStatus;
import com.vincentwang.ecommerce.order.service.IOrderService;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"order.events.enabled=false", "order.expiry.enabled=false"})
class OrderWorkflowIntegrationTest {
    private static final String USER_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
    private static final String OTHER_USER_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaab";
    private static final String PRODUCT_OWNER_ID = "cccccccc-cccc-4ccc-8ccc-cccccccccccc";

    @SuppressWarnings("resource")
    @Container
    static final MySQLContainer MYSQL_0 = new MySQLContainer("mysql:8.4.11")
            .withDatabaseName("order_db_0")
            .withUsername("ecommerce")
            .withPassword("test-password");

    @SuppressWarnings("resource")
    @Container
    static final MySQLContainer MYSQL_1 = new MySQLContainer("mysql:8.4.11")
            .withDatabaseName("order_db_1")
            .withUsername("ecommerce")
            .withPassword("test-password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("order.datasource.shard-0.url", MYSQL_0::getJdbcUrl);
        registry.add("order.datasource.shard-1.url", MYSQL_1::getJdbcUrl);
        registry.add("order.datasource.username", MYSQL_0::getUsername);
        registry.add("order.datasource.password", MYSQL_0::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IOrderService orderService;

    @MockitoBean
    private IInventoryFeignClient inventoryClient;

    @MockitoBean
    private ICatalogFeignClient catalogClient;

    private RestClient client;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM processed_event");
        jdbcTemplate.update("DELETE FROM order_outbox");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM orders");
        client = RestClient.create("http://localhost:" + port);
        when(catalogClient.getProduct(anyLong()))
                .thenReturn(new FeignProductResponse(101, PRODUCT_OWNER_ID, "Flash Laptop", 2_500, "USD", "ACTIVE"));
        when(inventoryClient.tryReservations(any())).thenAnswer(invocation -> {
            FeignInventoryTryRequest request = invocation.getArgument(0);
            return request.items().stream()
                    .map(item ->
                            reservation(request.orderId(), request.userId(), item.productId(), item.quantity(), "TRY"))
                    .toList();
        });
        when(inventoryClient.confirm(anyLong()))
                .thenAnswer(
                        invocation -> List.of(reservation(invocation.getArgument(0), USER_ID, 101, 2, "CONFIRMED")));
        when(inventoryClient.cancel(anyLong()))
                .thenAnswer(
                        invocation -> List.of(reservation(invocation.getArgument(0), USER_ID, 101, 2, "CANCELLED")));
    }

    @Test
    void acceptsIdempotentlyThenProcessesPaymentAndConfirmation() {
        String idempotencyKey = UUID.randomUUID().toString();
        var firstEntity = create(idempotencyKey);
        var duplicateEntity = create(idempotencyKey);

        assertThat(firstEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        OrderResponse accepted = Objects.requireNonNull(firstEntity.getBody());
        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(accepted.totalMinor()).isEqualTo(5_000);
        assertThat(Objects.requireNonNull(duplicateEntity.getBody()).orderId()).isEqualTo(accepted.orderId());
        verify(inventoryClient, times(1)).tryReservations(any());

        orderService.activate(UUID.randomUUID().toString(), USER_ID, accepted.orderId());
        orderService.activate(UUID.randomUUID().toString(), USER_ID, accepted.orderId());
        assertThat(get(accepted.orderId()).status()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        OrderResponse paid = client.post()
                .uri("/api/v1/orders/{orderId}/payment", accepted.orderId())
                .header(PlatformHttpHeaders.USER_ID, USER_ID)
                .header("Idempotency-Key", "payment-1")
                .retrieve()
                .body(OrderResponse.class);
        assertThat(paid.status()).isEqualTo(OrderStatus.CONFIRMING_INVENTORY);

        String commandEventId = UUID.randomUUID().toString();
        orderService.applyInventoryCommand(commandEventId, "inventory.confirm", USER_ID, accepted.orderId());
        orderService.applyInventoryCommand(commandEventId, "inventory.confirm", USER_ID, accepted.orderId());
        assertThat(get(accepted.orderId()).status()).isEqualTo(OrderStatus.PAID);
        verify(inventoryClient, times(1)).confirm(accepted.orderId());

        Integer eventCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_outbox", Integer.class);
        assertThat(eventCount).isEqualTo(2);
    }

    @Test
    void expiresPendingOrderAndCancelsInventoryIdempotently() {
        OrderResponse accepted = Objects.requireNonNull(create("expiry-key").getBody());
        orderService.activate(UUID.randomUUID().toString(), USER_ID, accepted.orderId());
        jdbcTemplate.update(
                "UPDATE orders SET payment_deadline = CURRENT_TIMESTAMP(6) - INTERVAL 1 SECOND WHERE user_id = ? AND order_id = ?",
                USER_ID,
                accepted.orderId());

        assertThat(orderService.expireBatch(10)).isEqualTo(1);
        assertThat(orderService.expireBatch(10)).isZero();
        assertThat(get(accepted.orderId()).status()).isEqualTo(OrderStatus.EXPIRED);

        String commandEventId = UUID.randomUUID().toString();
        orderService.applyInventoryCommand(commandEventId, "inventory.cancel", USER_ID, accepted.orderId());
        orderService.applyInventoryCommand(commandEventId, "inventory.cancel", USER_ID, accepted.orderId());
        verify(inventoryClient, times(1)).cancel(accepted.orderId());
    }

    @Test
    void rejectsPaymentBeforeOrderRequestedEventIsConsumed() {
        OrderResponse accepted = Objects.requireNonNull(create("early-payment").getBody());

        var status = client.post()
                .uri("/api/v1/orders/{orderId}/payment", accepted.orderId())
                .header(PlatformHttpHeaders.USER_ID, USER_ID)
                .header("Idempotency-Key", "payment-early")
                .exchange((request, response) -> response.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void pricesTheOrderFromTheCatalogAndSnapshotsItsNameAndCurrency() {
        OrderResponse accepted =
                Objects.requireNonNull(create("priced-by-catalog").getBody());

        assertThat(accepted.totalMinor()).isEqualTo(5_000);
        assertThat(accepted.currency()).isEqualTo("USD");
        assertThat(accepted.items()).singleElement().satisfies(item -> {
            assertThat(item.productName()).isEqualTo("Flash Laptop");
            assertThat(item.unitPriceMinor()).isEqualTo(2_500);
        });
    }

    @Test
    void createsOneOrderWithMultipleItemsAndOneBatchReservation() {
        when(catalogClient.getProduct(anyLong())).thenAnswer(invocation -> {
            long productId = invocation.getArgument(0);
            return new FeignProductResponse(
                    productId,
                    PRODUCT_OWNER_ID,
                    "Product " + productId,
                    productId == 101 ? 2_500 : 1_200,
                    "USD",
                    "ACTIVE");
        });

        OrderResponse order = Objects.requireNonNull(client.post()
                .uri("/api/v1/orders")
                .header(PlatformHttpHeaders.USER_ID, USER_ID)
                .header("Idempotency-Key", "multi-item-order")
                .body(new CreateOrderRequest(
                        List.of(new CreateOrderItemRequest(101, 2), new CreateOrderItemRequest(102, 3))))
                .retrieve()
                .body(OrderResponse.class));

        assertThat(order.items()).hasSize(2);
        assertThat(order.totalMinor()).isEqualTo(8_600);
        var requestCaptor = org.mockito.ArgumentCaptor.forClass(FeignInventoryTryRequest.class);
        verify(inventoryClient).tryReservations(requestCaptor.capture());
        assertThat(requestCaptor.getValue().items())
                .containsExactly(new FeignInventoryReservationItem(101, 2), new FeignInventoryReservationItem(102, 3));
    }

    @Test
    void refusesProductsThatAreNotOnSaleWithoutHoldingStock() {
        when(catalogClient.getProduct(anyLong()))
                .thenReturn(new FeignProductResponse(101, PRODUCT_OWNER_ID, "Flash Laptop", 2_500, "USD", "INACTIVE"));

        var status = createStatus("inactive-product");

        assertThat(status).isEqualTo(HttpStatus.CONFLICT);
        verify(inventoryClient, never()).tryReservations(any());
    }

    @Test
    void refusesAUsersOwnProductWithoutHoldingStock() {
        when(catalogClient.getProduct(anyLong()))
                .thenReturn(new FeignProductResponse(101, USER_ID, "My Laptop", 2_500, "USD", "ACTIVE"));

        var status = createStatus("self-purchase");

        assertThat(status).isEqualTo(HttpStatus.CONFLICT);
        verify(inventoryClient, never()).tryReservations(any());
    }

    @Test
    void reportsUnknownProductsAsUnavailableAndAnOutageAsRetryable() {
        doThrow(feignFailure(404)).when(catalogClient).getProduct(anyLong());
        assertThat(createStatus("unknown-product")).isEqualTo(HttpStatus.CONFLICT);

        doThrow(feignFailure(503)).when(catalogClient).getProduct(anyLong());
        assertThat(createStatus("catalog-down")).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(inventoryClient, never()).tryReservations(any());
    }

    @Test
    void translatesInventoryRefusalAndOutageIntoDistinctStatuses() {
        doThrow(feignFailure(409)).when(inventoryClient).tryReservations(any());
        assertThat(createStatus("no-stock")).isEqualTo(HttpStatus.CONFLICT);

        doThrow(feignFailure(500)).when(inventoryClient).tryReservations(any());
        assertThat(createStatus("inventory-down")).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void routesOrdersToTheExpectedDatabaseAndTable() throws SQLException {
        OrderResponse evenUser =
                Objects.requireNonNull(createForUser(USER_ID, "route-even").getBody());
        OrderResponse oddUser =
                Objects.requireNonNull(createForUser(OTHER_USER_ID, "route-odd").getBody());

        assertThat(physicalOrderCount(MYSQL_0, evenUser.orderId())).isEqualTo(1);
        assertThat(physicalOrderCount(MYSQL_1, evenUser.orderId())).isZero();
        assertThat(physicalOrderCount(MYSQL_1, oddUser.orderId())).isEqualTo(1);
        assertThat(physicalOrderCount(MYSQL_0, oddUser.orderId())).isZero();

        var wrongCustomerStatus = client.get()
                .uri("/api/v1/orders/{orderId}", oddUser.orderId())
                .header(PlatformHttpHeaders.USER_ID, USER_ID)
                .exchange((request, response) -> response.getStatusCode());
        assertThat(wrongCustomerStatus).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private org.springframework.http.ResponseEntity<OrderResponse> create(String idempotencyKey) {
        return createForUser(USER_ID, idempotencyKey);
    }

    private org.springframework.http.ResponseEntity<OrderResponse> createForUser(String userId, String idempotencyKey) {
        return client.post()
                .uri("/api/v1/orders")
                .header(PlatformHttpHeaders.USER_ID, userId)
                .header("Idempotency-Key", idempotencyKey)
                .body(new CreateOrderRequest(List.of(new CreateOrderItemRequest(101, 2))))
                .retrieve()
                .toEntity(OrderResponse.class);
    }

    private org.springframework.http.HttpStatusCode createStatus(String idempotencyKey) {
        return client.post()
                .uri("/api/v1/orders")
                .header(PlatformHttpHeaders.USER_ID, USER_ID)
                .header("Idempotency-Key", idempotencyKey)
                .body(new CreateOrderRequest(List.of(new CreateOrderItemRequest(101, 2))))
                .exchange((request, response) -> response.getStatusCode());
    }

    private static FeignException feignFailure(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/x", java.util.Map.of(), null, new RequestTemplate());
        return FeignException.errorStatus(
                "test",
                feign.Response.builder()
                        .status(status)
                        .reason("test")
                        .request(request)
                        .headers(java.util.Map.of())
                        .build());
    }

    private static int physicalOrderCount(MySQLContainer container, long orderId) throws SQLException {
        String table = "orders_" + Math.floorMod(orderId, 2);
        try (Connection connection = DriverManager.getConnection(
                        container.getJdbcUrl(), container.getUsername(), container.getPassword());
                PreparedStatement statement =
                        connection.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE order_id = ?")) {
            statement.setLong(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private OrderResponse get(long orderId) {
        return Objects.requireNonNull(client.get()
                .uri("/api/v1/orders/{orderId}", orderId)
                .header(PlatformHttpHeaders.USER_ID, USER_ID)
                .retrieve()
                .body(OrderResponse.class));
    }

    private static FeignInventoryReservationResponse reservation(
            long orderId, String userId, long productId, long quantity, String status) {
        return new FeignInventoryReservationResponse(
                orderId, userId, productId, quantity, status, Instant.now().plusSeconds(900));
    }
}
