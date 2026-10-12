package com.vincentwang.ecommerce.catalog.application.repository;

import com.vincentwang.ecommerce.catalog.application.model.ProductSearchDocument;
import com.vincentwang.ecommerce.catalog.application.model.ProductSearchResult;

public interface IProductSearchRepository {

    ProductSearchResult search(String query, int page, int size);

    void index(ProductSearchDocument document);
}
