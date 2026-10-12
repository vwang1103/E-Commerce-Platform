package com.vincentwang.ecommerce.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vincentwang.ecommerce.catalog.api.dto.request.CreateProductRequest;
import com.vincentwang.ecommerce.catalog.api.dto.request.UpdateProductRequest;
import com.vincentwang.ecommerce.catalog.application.exception.ProductVersionConflictException;
import com.vincentwang.ecommerce.catalog.application.repository.ICategoryRepository;
import com.vincentwang.ecommerce.catalog.application.repository.IProductRepository;
import com.vincentwang.ecommerce.catalog.domain.model.Product;
import com.vincentwang.ecommerce.catalog.domain.model.ProductStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogApplicationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 10, 12, 0);
    private static final String USER_ID = "0337c503-6233-385f-ab7b-0a4c48e29513";

    @Mock
    private IProductRepository productRepository;

    @Mock
    private ICategoryRepository categoryRepository;

    private ICatalogService catalogService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC);
        catalogService = new CatalogApplicationService(
                productRepository, categoryRepository, Optional.empty(), Optional.empty(), clock);
    }

    @Test
    void createsNormalizedProductAndOutboxEvent() {
        when(productRepository.categoryExists(100L)).thenReturn(true);
        doAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setProductId(42L);
                    return 1;
                })
                .when(productRepository)
                .insert(any(Product.class));
        when(productRepository.findById(42L)).thenReturn(Optional.of(product(42L, 0L)));

        var product = catalogService.createProduct(
                USER_ID,
                new CreateProductRequest(100L, "Laptop", "Developer laptop", 129_900L, "usd", ProductStatus.ACTIVE));

        assertThat(product.productId()).isEqualTo(42L);
        assertThat(product.currency()).isEqualTo("USD");
        verify(productRepository)
                .appendOutboxEvent(any(String.class), eq("product.created"), any(Product.class), eq(NOW));
    }

    @Test
    void reportsOptimisticLockConflict() {
        when(productRepository.categoryExists(100L)).thenReturn(true);
        when(productRepository.update(any(Product.class))).thenReturn(0);
        when(productRepository.findById(42L)).thenReturn(Optional.of(product(42L, 1L)));

        var request = new UpdateProductRequest(100L, "Laptop", "Changed", 129_900L, "USD", ProductStatus.ACTIVE, 0L);

        assertThatThrownBy(() -> catalogService.updateProduct(USER_ID, 42L, request))
                .isInstanceOf(ProductVersionConflictException.class);
    }

    private static Product product(long productId, long version) {
        return new Product(
                productId,
                USER_ID,
                100L,
                "Laptop",
                "Developer laptop",
                129_900L,
                "USD",
                ProductStatus.ACTIVE,
                version,
                NOW,
                NOW);
    }
}
