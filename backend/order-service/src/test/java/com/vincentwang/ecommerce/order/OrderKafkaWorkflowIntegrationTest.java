package com.vincentwang.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vincentwang.ecommerce.common.messaging.DeadLetterReplayResult;
import com.vincentwang.ecommerce.order.api.dto.request.CreateOrderItemRequest;
import com.vincentwang.ecommerce.order.api.dto.request.CreateOrderRequest;
import com.vincentwang.ecommerce.order.api.dto.response.OrderResponse;
import com.vincentwang.ecommerce.order.application.IOrderService;
import com.vincentwang.ecommerce.order.domain.model.OrderStatus;
import com.vincentwang.ecommerce.order.infrastructure.client.ICatalogFeignClient;
import com.vincentwang.ecommerce.order.infrastructure.client.IInventoryFeignClient;
import com.vincentwang.ecommerce.order.infrastructure.client.dto.FeignInventoryReservationResponse;
import com.vincentwang.ecommerce.order.infrastructure.client.dto.FeignInventoryTryRequest;
import com.vincentwang.ecommerce.order.infrastructure.client.dto.FeignProductResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "order.events.enabled=true",
            "order.expiry.enabled=false",
            "ecommerce.kafka.dlt.replay-enabled=true",
            "ecommerce.kafka.dlt.retry.initial-interval=25ms",
            "ecommerce.kafka.dlt.retry.max-interval=50ms",
            "ecommerce.kafka.dlt.retry.max-attempts=2"
        })
class OrderKafkaWorkflowIntegrationTest {
    private static final String USER_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
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

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.3.1");

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("order.datasource.shard-0.url", MYSQL_0::getJdbcUrl);
        registry.add("order.datasource.shard-1.url", MYSQL_1::getJdbcUrl);
        registry.add("order.datasource.username", MYSQL_0::getUsername);
        registry.add("order.datasource.password", MYSQL_0::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("order.events.workflow-topic", () -> "order-workflow-" + TOPIC_SUFFIX);
        registry.add("order.events.inventory-topic", () -> "inventory-command-" + TOPIC_SUFFIX);
        registry.add("order.events.workflow-consumer-group", () -> "workflow-" + TOPIC_SUFFIX);
        registry.add("order.events.inventory-consumer-group", () -> "inventory-" + TOPIC_SUFFIX);
    }

    private static final String TOPIC_SUFFIX = UUID.randomUUID().toString();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @LocalServerPort
    private int port;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    @Autowired
    private DefaultErrorHandler deadLetterErrorHandler;

    @MockitoBean
    private IInventoryFeignClient inventoryClient;

    @MockitoBean
    private ICatalogFeignClient catalogClient;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM processed_event");
        jdbcTemplate.update("DELETE FROM order_outbox");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM orders");
        when(catalogClient.getProduct(anyLong()))
                .thenAnswer(invocation -> new FeignProductResponse(
                        invocation.getArgument(0), PRODUCT_OWNER_ID, "Test Product", 99_900, "USD", "ACTIVE"));
        when(inventoryClient.tryReservations(any())).thenAnswer(invocation -> {
            FeignInventoryTryRequest request = invocation.getArgument(0);
            return request.items().stream()
                    .map(item ->
                            reservation(request.orderId(), request.userId(), item.productId(), item.quantity(), "TRY"))
                    .toList();
        });
        when(inventoryClient.confirm(anyLong()))
                .thenAnswer(
                        invocation -> List.of(reservation(invocation.getArgument(0), USER_ID, 101, 1, "CONFIRMED")));
    }

    @Test
    void publishesAndConsumesTheCompletePaidOrderWorkflow() throws Exception {
        assertThat(listenerRegistry.getListenerContainers()).allSatisfy(container -> {
            assertThat(container).isInstanceOf(AbstractMessageListenerContainer.class);
            AbstractMessageListenerContainer<?, ?> messageListenerContainer =
                    (AbstractMessageListenerContainer<?, ?>) container;
            assertThat(messageListenerContainer.getCommonErrorHandler()).isSameAs(deadLetterErrorHandler);
        });
        OrderResponse accepted = orderService.submit(
                USER_ID, new CreateOrderRequest(List.of(new CreateOrderItemRequest(101, 1))), "kafka-order-key");

        OrderResponse pending =
                awaitOrderStatus(accepted.orderId(), OrderStatus.PENDING_PAYMENT, Duration.ofSeconds(10));
        assertThat(pending.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        orderService.pay(USER_ID, accepted.orderId(), "kafka-payment-key");

        verify(inventoryClient, timeout(10_000).times(1)).confirm(accepted.orderId());
        awaitOrderStatus(accepted.orderId(), OrderStatus.PAID, Duration.ofSeconds(10));
        Integer unpublished = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_outbox WHERE published_at IS NULL", Integer.class);
        assertThat(unpublished).isZero();
    }

    @Test
    void publishesFailedCommandToDltAndReplaysTheOriginalRecord() throws Exception {
        OrderResponse accepted = orderService.submit(
                USER_ID, new CreateOrderRequest(List.of(new CreateOrderItemRequest(102, 1))), "dlt-order-key");
        awaitOrderStatus(accepted.orderId(), OrderStatus.PENDING_PAYMENT, Duration.ofSeconds(10));

        when(inventoryClient.confirm(accepted.orderId())).thenThrow(new IllegalStateException("inventory unavailable"));
        orderService.pay(USER_ID, accepted.orderId(), "dlt-payment-key");

        ConsumerRecord<String, String> deadLetter =
                awaitDeadLetter("inventory-command-" + TOPIC_SUFFIX + ".DLT", Duration.ofSeconds(15));
        assertThat(deadLetter.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC))
                .isNotNull();
        awaitOrderStatus(accepted.orderId(), OrderStatus.INVENTORY_CONFIRMATION_FAILED, Duration.ofSeconds(10));

        AtomicInteger successfulReplayCalls = new AtomicInteger();
        doAnswer(invocation -> {
                    successfulReplayCalls.incrementAndGet();
                    return List.of(reservation(accepted.orderId(), USER_ID, 102, 1, "CONFIRMED"));
                })
                .when(inventoryClient)
                .confirm(accepted.orderId());

        DeadLetterReplayResult replay = RestClient.create("http://localhost:" + port)
                .post()
                .uri("/internal/v1/events/dlt/replay")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReplayRequest(deadLetter.topic(), deadLetter.partition(), deadLetter.offset()))
                .retrieve()
                .body(DeadLetterReplayResult.class);
        assertThat(replay).isNotNull();
        assertThat(replay.destinationTopic()).isEqualTo("inventory-command-" + TOPIC_SUFFIX);
        await(Duration.ofSeconds(10), () -> successfulReplayCalls.get() == 1 ? true : null);
        awaitOrderStatus(accepted.orderId(), OrderStatus.PAID, Duration.ofSeconds(10));
    }

    private OrderResponse awaitOrderStatus(long orderId, OrderStatus status, Duration timeout) throws Exception {
        return await(timeout, () -> {
            OrderResponse order = orderService.get(USER_ID, orderId);
            return order.status() == status ? order : null;
        });
    }

    private static <T> T await(Duration timeout, Supplier<T> supplier) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            T value = supplier.get();
            if (value != null) {
                return value;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Condition was not met within " + timeout);
    }

    private ConsumerRecord<String, String> awaitDeadLetter(String topic, Duration timeout) throws Exception {
        try (Consumer<String, String> consumer = consumerFactory.createConsumer("dlt-test-" + UUID.randomUUID(), "")) {
            consumer.subscribe(Set.of(topic));
            return await(timeout, () -> firstRecord(consumer, topic));
        }
    }

    private static ConsumerRecord<String, String> firstRecord(Consumer<String, String> consumer, String topic) {
        for (ConsumerRecord<String, String> record :
                consumer.poll(Duration.ofMillis(250)).records(topic)) {
            return record;
        }
        return null;
    }

    private static FeignInventoryReservationResponse reservation(
            long orderId, String userId, long productId, long quantity, String status) {
        return new FeignInventoryReservationResponse(
                orderId, userId, productId, quantity, status, Instant.now().plusSeconds(900));
    }

    private record ReplayRequest(String topic, int partition, long offset) {}
}
