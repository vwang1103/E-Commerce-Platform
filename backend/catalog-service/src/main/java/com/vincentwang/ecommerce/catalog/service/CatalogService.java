package com.vincentwang.ecommerce.catalog.service;

import com.vincentwang.ecommerce.catalog.controller.dto.request.CreateProductRequest;
import com.vincentwang.ecommerce.catalog.controller.dto.request.UpdateProductRequest;
import com.vincentwang.ecommerce.catalog.controller.dto.response.CategoryResponse;
import com.vincentwang.ecommerce.catalog.controller.dto.response.ProductResponse;
import com.vincentwang.ecommerce.catalog.messaging.ProductEvents;
import com.vincentwang.ecommerce.catalog.model.CurrencyCode;
import com.vincentwang.ecommerce.catalog.model.Product;
import com.vincentwang.ecommerce.catalog.model.ProductSearchResult;
import com.vincentwang.ecommerce.catalog.repository.ICategoryRepository;
import com.vincentwang.ecommerce.catalog.repository.IProductRepository;
import com.vincentwang.ecommerce.catalog.repository.IProductSearchBenchmarkRepository;
import com.vincentwang.ecommerce.catalog.repository.IProductSearchRepository;
import com.vincentwang.ecommerce.catalog.service.exception.InvalidCategoryException;
import com.vincentwang.ecommerce.catalog.service.exception.ProductNotFoundException;
import com.vincentwang.ecommerce.catalog.service.exception.ProductOwnershipException;
import com.vincentwang.ecommerce.catalog.service.exception.ProductVersionConflictException;
import com.vincentwang.ecommerce.common.pagination.PageResult;
import com.vincentwang.ecommerce.common.pagination.Pagination;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService implements ICatalogService {

    private final IProductRepository productRepository;
    private final ICategoryRepository categoryRepository;
    private final Optional<IProductSearchRepository> productSearchRepository;
    private final Optional<IProductSearchBenchmarkRepository> productSearchBenchmarkRepository;
    private final Clock clock;

    @Autowired
    public CatalogService(
            IProductRepository productRepository,
            ICategoryRepository categoryRepository,
            Optional<IProductSearchRepository> productSearchRepository,
            Optional<IProductSearchBenchmarkRepository> productSearchBenchmarkRepository) {
        this(
                productRepository,
                categoryRepository,
                productSearchRepository,
                productSearchBenchmarkRepository,
                Clock.systemUTC());
    }

    public CatalogService(
            IProductRepository productRepository,
            ICategoryRepository categoryRepository,
            Optional<IProductSearchRepository> productSearchRepository,
            Optional<IProductSearchBenchmarkRepository> productSearchBenchmarkRepository,
            Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productSearchRepository = productSearchRepository;
        this.productSearchBenchmarkRepository = productSearchBenchmarkRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @Override
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    @Override
    public ProductResponse createProduct(String userId, CreateProductRequest request) {
        requireCategory(request.categoryId());
        Product product = new Product(
                null,
                userId,
                request.categoryId(),
                request.name().trim(),
                request.description().trim(),
                request.priceMinor(),
                CurrencyCode.of(request.currency()).value(),
                request.status(),
                0L,
                null,
                null);
        productRepository.insert(product);
        Product saved = requireProduct(product.getProductId());
        writeOutbox(ProductEvents.CREATED, saved);
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public ProductResponse getProduct(long productId) {
        return ProductResponse.from(requireProduct(productId));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResult<ProductResponse> listProducts(String userId, int page, int size) {
        long offset = Pagination.offset(page, size);
        List<ProductResponse> products = productRepository.findPage(userId, offset, size).stream()
                .map(ProductResponse::from)
                .toList();
        return new PageResult<>(products, page, size, productRepository.count(userId));
    }

    @Transactional
    @Override
    public ProductResponse updateProduct(String userId, long productId, UpdateProductRequest request) {
        requireCategory(request.categoryId());
        Product existing = requireProduct(productId);
        if (!existing.getUserId().equals(userId)) {
            throw new ProductOwnershipException(productId);
        }
        Product product = new Product(
                productId,
                userId,
                request.categoryId(),
                request.name().trim(),
                request.description().trim(),
                request.priceMinor(),
                CurrencyCode.of(request.currency()).value(),
                request.status(),
                request.version(),
                null,
                null);
        if (productRepository.update(product) == 0) {
            if (productRepository.findById(productId).isEmpty()) {
                throw new ProductNotFoundException(productId);
            }
            throw new ProductVersionConflictException(productId);
        }
        Product saved = requireProduct(productId);
        writeOutbox(ProductEvents.UPDATED, saved);
        return ProductResponse.from(saved);
    }

    @Override
    public Optional<ProductSearchResult> searchProducts(String query, int page, int size) {
        return productSearchRepository.map(repository -> repository.search(query, page, size));
    }

    @Override
    public Optional<ProductSearchResult> benchmarkMySqlSearch(String query, int page, int size) {
        return productSearchBenchmarkRepository.map(repository -> repository.search(query, page, size));
    }

    private Product requireProduct(long productId) {
        return productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void requireCategory(long categoryId) {
        if (!productRepository.categoryExists(categoryId)) {
            throw new InvalidCategoryException(categoryId);
        }
    }

    private void writeOutbox(String eventType, Product product) {
        productRepository.appendOutboxEvent(UUID.randomUUID().toString(), eventType, product, LocalDateTime.now(clock));
    }
}
