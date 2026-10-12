package com.vincentwang.ecommerce.order.application;

import com.vincentwang.ecommerce.order.application.model.ProductQuote;

/** Port to the catalog, which is the only authority on product names and prices. */
public interface IProductCatalogClient {

    /**
     * @throws com.vincentwang.ecommerce.order.application.exception.ProductUnavailableException if the product is unknown
     * @throws com.vincentwang.ecommerce.order.application.exception.DependencyUnavailableException if the catalog cannot answer
     */
    ProductQuote quote(long productId);
}
