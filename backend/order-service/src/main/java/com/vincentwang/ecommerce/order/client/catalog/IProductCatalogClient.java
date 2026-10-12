package com.vincentwang.ecommerce.order.client.catalog;

import com.vincentwang.ecommerce.order.model.ProductQuote;

/** Port to the catalog, which is the only authority on product names and prices. */
public interface IProductCatalogClient {

    /**
     * @throws com.vincentwang.ecommerce.order.service.exception.ProductUnavailableException if the product is unknown
     * @throws com.vincentwang.ecommerce.order.service.exception.DependencyUnavailableException if the catalog cannot answer
     */
    ProductQuote quote(long productId);
}
