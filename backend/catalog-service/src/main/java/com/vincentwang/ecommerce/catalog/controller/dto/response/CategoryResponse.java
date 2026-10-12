package com.vincentwang.ecommerce.catalog.controller.dto.response;

import com.vincentwang.ecommerce.catalog.model.Category;

public record CategoryResponse(long categoryId, Long parentCategoryId, String name, String slug) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.categoryId(), category.parentCategoryId(), category.name(), category.slug());
    }
}
