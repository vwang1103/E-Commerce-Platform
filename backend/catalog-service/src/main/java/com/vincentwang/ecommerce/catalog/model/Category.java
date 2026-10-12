package com.vincentwang.ecommerce.catalog.model;

/** A product grouping shown in storefront navigation. */
public record Category(long categoryId, Long parentCategoryId, String name, String slug) {}
