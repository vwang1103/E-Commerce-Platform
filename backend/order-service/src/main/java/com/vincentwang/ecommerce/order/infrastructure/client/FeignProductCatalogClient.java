package com.vincentwang.ecommerce.order.infrastructure.client;

import com.vincentwang.ecommerce.order.application.IProductCatalogClient;
import com.vincentwang.ecommerce.order.application.exception.DependencyUnavailableException;
import com.vincentwang.ecommerce.order.application.exception.ProductUnavailableException;
import com.vincentwang.ecommerce.order.application.model.ProductQuote;
import com.vincentwang.ecommerce.order.infrastructure.client.dto.FeignProductResponse;
import feign.FeignException;
import org.springframework.stereotype.Component;

/** Keeps Feign types out of the application layer by translating HTTP outcomes into domain failures. */
@Component
public class FeignProductCatalogClient implements IProductCatalogClient {

    private static final String ACTIVE = "ACTIVE";

    private final ICatalogFeignClient client;

    public FeignProductCatalogClient(ICatalogFeignClient client) {
        this.client = client;
    }

    @Override
    public ProductQuote quote(long productId) {
        try {
            FeignProductResponse product = client.getProduct(productId);
            return new ProductQuote(
                    product.productId(),
                    product.userId(),
                    product.name(),
                    product.priceMinor(),
                    product.currency(),
                    ACTIVE.equals(product.status()));
        } catch (FeignException.NotFound exception) {
            throw new ProductUnavailableException(productId);
        } catch (FeignException exception) {
            throw new DependencyUnavailableException("catalog", exception);
        }
    }
}
