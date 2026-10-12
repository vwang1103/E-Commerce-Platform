package com.vincentwang.ecommerce.catalog.application.repository;

import com.vincentwang.ecommerce.catalog.application.model.ProductSearchResult;

public interface IProductSearchBenchmarkRepository {

    ProductSearchResult search(String query, int page, int size);
}
