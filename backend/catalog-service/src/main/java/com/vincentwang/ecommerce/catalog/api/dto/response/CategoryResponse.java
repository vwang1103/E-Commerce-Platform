package com.vincentwang.ecommerce.catalog.api.dto.response;

import com.vincentwang.ecommerce.catalog.domain.model.Category;

public record CategoryResponse(long categoryId, Long parentCategoryId, String name, String slug) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.categoryId(), category.parentCategoryId(), category.name(), category.slug());
    }
}
