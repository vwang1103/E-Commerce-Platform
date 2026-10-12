package com.vincentwang.ecommerce.catalog.application;

import com.vincentwang.ecommerce.catalog.api.dto.request.CreateProductRequest;
import com.vincentwang.ecommerce.catalog.api.dto.request.UpdateProductRequest;
import com.vincentwang.ecommerce.catalog.api.dto.response.CategoryResponse;
import com.vincentwang.ecommerce.catalog.api.dto.response.ProductResponse;
import com.vincentwang.ecommerce.catalog.application.model.ProductSearchResult;
import com.vincentwang.ecommerce.common.pagination.PageResult;
import java.util.List;
import java.util.Optional;

/** Single inbound application boundary for all catalog use cases. */
public interface ICatalogService {

    List<CategoryResponse> listCategories();

    ProductResponse createProduct(String userId, CreateProductRequest request);

    ProductResponse getProduct(long productId);

    PageResult<ProductResponse> listProducts(String userId, int page, int size);

    ProductResponse updateProduct(String userId, long productId, UpdateProductRequest request);

    Optional<ProductSearchResult> searchProducts(String query, int page, int size);

    Optional<ProductSearchResult> benchmarkMySqlSearch(String query, int page, int size);
}
