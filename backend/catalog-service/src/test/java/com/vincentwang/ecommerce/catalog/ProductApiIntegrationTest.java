package com.vincentwang.ecommerce.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincentwang.ecommerce.catalog.controller.dto.request.CreateProductRequest;
import com.vincentwang.ecommerce.catalog.controller.dto.request.UpdateProductRequest;
import com.vincentwang.ecommerce.catalog.controller.dto.response.CategoryResponse;
import com.vincentwang.ecommerce.catalog.controller.dto.response.ProductResponse;
import com.vincentwang.ecommerce.catalog.controller.dto.response.ProductSearchResponse;
import com.vincentwang.ecommerce.catalog.model.ProductStatus;
import com.vincentwang.ecommerce.catalog.repository.mybatis.IProductMapper;
import com.vincentwang.ecommerce.common.web.ApiPageResponse;
import com.vincentwang.ecommerce.common.web.PlatformHttpHeaders;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "catalog.search.mysql-benchmark.enabled=true")
class ProductApiIntegrationTest {

    private static final String USER_ID = "0337c503-6233-385f-ab7b-0a4c48e29513";
    private static final String OTHER_USER_ID = "d6c2be7b-8f38-3e6b-957f-984c576b87d4";

    @SuppressWarnings("resource")
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.11")
            .withDatabaseName("catalog_db")
            .withUsername("ecommerce")
            .withPassword("test-password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IProductMapper productMapper;

    private RestClient client;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM catalog_outbox");
        jdbcTemplate.update("DELETE FROM product");
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    void listsTheSeededCategoriesInIdOrder() {
        var categories = client.get()
                .uri("/api/v1/categories")
                .retrieve()
                .body(new ParameterizedTypeReference<java.util.List<CategoryResponse>>() {});

        assertThat(categories)
                .extracting((CategoryResponse category) -> category.categoryId())
                .startsWith(100L, 200L, 300L);
        assertThat(categories)
                .extracting((CategoryResponse category) -> category.slug())
                .contains("electronics", "home-and-kitchen");
    }

    @Test
    void createsReadsListsAndUpdatesProductWithTransactionalOutbox() {
        var createResponse = createProductEntity(USER_ID, 99_900L);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProductResponse created = Objects.requireNonNull(createResponse.getBody());
        assertThat(created.productId()).isPositive();
        assertThat(created.currency()).isEqualTo("USD");
        assertThat(createResponse.getHeaders().getLocation()).hasToString("/api/v1/products/" + created.productId());

        ProductResponse fetched = client.get()
                .uri("/api/v1/products/{id}", created.productId())
                .retrieve()
                .body(ProductResponse.class);
        assertThat(fetched).isEqualTo(created);

        ApiPageResponse<ProductResponse> page = client.get()
                .uri("/api/v1/products/mine?page=0&size=10")
                .header(PlatformHttpHeaders.USER_ID, USER_ID)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        assertThat(page).isNotNull();
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items()).hasSize(1);

        var updateRequest = new UpdateProductRequest(
                100L,
                "Updated Flash Sale Laptop",
                "Updated description",
                89_900L,
                "USD",
                ProductStatus.ACTIVE,
                created.version());

        ProductResponse updated = client.put()
                .uri("/api/v1/products/{id}", created.productId())
                .header(PlatformHttpHeaders.USER_ID, USER_ID)
                .body(updateRequest)
                .retrieve()
                .body(ProductResponse.class);
        assertThat(updated.name()).isEqualTo("Updated Flash Sale Laptop");
        assertThat(updated.version()).isEqualTo(1L);

        Integer outboxCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM catalog_outbox", Integer.class);
        assertThat(outboxCount).isEqualTo(2);
    }

    @Test
    void rejectsNegativePrice() {
        var status = client.post()
                .uri("/api/v1/products")
                .header(PlatformHttpHeaders.USER_ID, USER_ID)
                .body(productRequest(-1L))
                .exchange((request, response) -> response.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void returnsNotFoundForMissingProduct() {
        var status =
                client.get().uri("/api/v1/products/999999").exchange((request, response) -> response.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void paginatesInStableProductIdOrder() {
        ProductResponse first = createProduct(USER_ID, 1_000L);
        ProductResponse second = createProduct(USER_ID, 2_000L);
        createProduct(OTHER_USER_ID, 3_000L);

        ApiPageResponse<ProductResponse> page = client.get()
                .uri("/api/v1/products/mine?page=0&size=2")
                .header(PlatformHttpHeaders.USER_ID, USER_ID)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        assertThat(page).isNotNull();
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.items())
                .extracting((ProductResponse product) -> product.productId())
                .containsExactly(first.productId(), second.productId());
    }

    @Test
    void claimsAndMarksTransactionalOutboxEvents() {
        ProductResponse product = createProduct(USER_ID, 1_000L);

        var events = productMapper.findUnpublishedOutbox(10);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().aggregateId())
                .isEqualTo(product.productId().toString());
        assertThat(events.getFirst().payload()).contains("Laptop used by the catalog integration test");
        assertThat(productMapper.markOutboxPublished(events.getFirst().eventId()))
                .isEqualTo(1);
        assertThat(productMapper.findUnpublishedOutbox(10)).isEmpty();
    }

    @Test
    void exposesTheDisabledByDefaultMySqlSearchBaseline() {
        ProductResponse matching = createProduct(USER_ID, 1_000L);
        createProduct(OTHER_USER_ID, 2_000L);

        ProductSearchResponse result = client.get()
                .uri("/internal/benchmarks/search/mysql?q=laptop&page=0&size=20")
                .retrieve()
                .body(ProductSearchResponse.class);

        assertThat(result).isNotNull();
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.items().getFirst().productId()).isEqualTo(matching.productId());

        ProductSearchResponse escapedWildcard = client.get()
                .uri("/internal/benchmarks/search/mysql?q=%25&page=0&size=20")
                .retrieve()
                .body(ProductSearchResponse.class);
        assertThat(escapedWildcard).isNotNull();
        assertThat(escapedWildcard.totalElements()).isZero();
    }

    private ProductResponse createProduct(String userId, long priceMinor) {
        return Objects.requireNonNull(createProductEntity(userId, priceMinor).getBody());
    }

    private org.springframework.http.ResponseEntity<ProductResponse> createProductEntity(
            String userId, long priceMinor) {
        return client.post()
                .uri("/api/v1/products")
                .header(PlatformHttpHeaders.USER_ID, userId)
                .body(productRequest(priceMinor))
                .retrieve()
                .toEntity(ProductResponse.class);
    }

    private static CreateProductRequest productRequest(long priceMinor) {
        return new CreateProductRequest(
                100L,
                "Flash Sale Laptop",
                "Laptop used by the catalog integration test",
                priceMinor,
                "usd",
                ProductStatus.ACTIVE);
    }
}
