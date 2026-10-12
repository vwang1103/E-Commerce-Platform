package com.vincentwang.ecommerce.catalog.application.model;

import java.util.List;

/** Search-engine result plus timing used by the Elasticsearch/MySQL benchmark. */
public record ProductSearchResult(
        List<ProductSearchDocument> items, int page, int size, long totalElements, long tookMillis) {}
