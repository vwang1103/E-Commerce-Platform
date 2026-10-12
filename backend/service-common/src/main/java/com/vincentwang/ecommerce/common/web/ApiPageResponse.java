package com.vincentwang.ecommerce.common.web;

import com.vincentwang.ecommerce.common.pagination.Pagination;
import java.util.List;

public record ApiPageResponse<T>(List<T> items, int page, int size, long totalElements, long totalPages) {

    public ApiPageResponse {
        items = List.copyOf(items);
    }

    public static <T> ApiPageResponse<T> of(List<T> items, int page, int size, long totalElements) {
        long totalPages = Pagination.totalPages(totalElements, size);
        return new ApiPageResponse<>(items, page, size, totalElements, totalPages);
    }
}
