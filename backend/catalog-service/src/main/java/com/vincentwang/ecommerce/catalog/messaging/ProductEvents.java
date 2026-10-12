package com.vincentwang.ecommerce.catalog.messaging;

/** Event types the catalog writes to its outbox; consumers such as search indexing key off these. */
public final class ProductEvents {

    public static final String CREATED = "product.created";
    public static final String UPDATED = "product.updated";

    private ProductEvents() {}
}
