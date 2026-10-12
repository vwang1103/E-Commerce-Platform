package com.vincentwang.ecommerce.catalog.infrastructure.search;

import com.vincentwang.ecommerce.catalog.application.model.ProductSearchDocument;
import com.vincentwang.ecommerce.catalog.application.model.ProductSearchResult;
import com.vincentwang.ecommerce.catalog.application.repository.IProductSearchBenchmarkRepository;
import com.vincentwang.ecommerce.catalog.domain.model.Product;
import com.vincentwang.ecommerce.catalog.infrastructure.persistence.IProductMapper;
import com.vincentwang.ecommerce.common.pagination.Pagination;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "catalog.search.mysql-benchmark", name = "enabled", havingValue = "true")
public class MySqlLikeProductSearchBenchmark implements IProductSearchBenchmarkRepository {

    private final IProductMapper productMapper;

    public MySqlLikeProductSearchBenchmark(IProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    @Override
    public ProductSearchResult search(String query, int page, int size) {
        long startedAt = System.nanoTime();
        String escapedQuery = escapeLike(query);
        long offset = Pagination.offset(page, size);
        List<ProductSearchDocument> documents = productMapper.searchLike(escapedQuery, offset, size).stream()
                .map(MySqlLikeProductSearchBenchmark::toSearchDocument)
                .toList();
        long total = productMapper.countLike(escapedQuery);
        long tookMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        return new ProductSearchResult(documents, page, size, total, tookMillis);
    }

    private static String escapeLike(String query) {
        return query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static ProductSearchDocument toSearchDocument(Product product) {
        return new ProductSearchDocument(
                product.getProductId(),
                product.getUserId(),
                product.getCategoryId(),
                product.getName(),
                product.getDescription(),
                product.getPriceMinor(),
                product.getCurrency(),
                product.getStatus(),
                product.getVersion());
    }
}
