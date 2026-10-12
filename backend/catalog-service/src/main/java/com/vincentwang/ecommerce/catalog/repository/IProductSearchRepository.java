package com.vincentwang.ecommerce.catalog.repository;

import com.vincentwang.ecommerce.catalog.model.ProductSearchDocument;
import com.vincentwang.ecommerce.catalog.model.ProductSearchResult;

public interface IProductSearchRepository {

    ProductSearchResult search(String query, int page, int size);

    void index(ProductSearchDocument document);
}
