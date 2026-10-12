package com.vincentwang.ecommerce.catalog.api.dto.response;

import com.vincentwang.ecommerce.catalog.application.model.ProductSearchDocument;
import com.vincentwang.ecommerce.catalog.application.model.ProductSearchResult;
import com.vincentwang.ecommerce.common.pagination.Pagination;
import java.util.List;

public record ProductSearchResponse(
        List<ProductSearchDocument> items, int page, int size, long totalElements, int totalPages, long tookMillis) {

    public static ProductSearchResponse of(
            List<ProductSearchDocument> items, int page, int size, long totalElements, long tookMillis) {
        int totalPages = Math.toIntExact(Pagination.totalPages(totalElements, size));
        return new ProductSearchResponse(items, page, size, totalElements, totalPages, tookMillis);
    }

    public static ProductSearchResponse from(ProductSearchResult result) {
        return of(result.items(), result.page(), result.size(), result.totalElements(), result.tookMillis());
    }
}
