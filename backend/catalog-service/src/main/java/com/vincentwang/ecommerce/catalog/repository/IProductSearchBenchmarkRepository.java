package com.vincentwang.ecommerce.catalog.repository;

import com.vincentwang.ecommerce.catalog.model.ProductSearchResult;

public interface IProductSearchBenchmarkRepository {

    ProductSearchResult search(String query, int page, int size);
}
