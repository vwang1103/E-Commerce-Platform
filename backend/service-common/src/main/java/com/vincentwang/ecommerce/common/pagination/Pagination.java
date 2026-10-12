package com.vincentwang.ecommerce.common.pagination;

public final class Pagination {

    private Pagination() {}

    public static long offset(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        return Math.multiplyExact((long) page, size);
    }

    public static int offsetAsInt(int page, int size) {
        return Math.toIntExact(offset(page, size));
    }

    public static long totalPages(long totalElements, int size) {
        if (totalElements < 0) {
            throw new IllegalArgumentException("Total elements must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        return totalElements == 0 ? 0 : Math.floorDiv(totalElements - 1, size) + 1;
    }
}
