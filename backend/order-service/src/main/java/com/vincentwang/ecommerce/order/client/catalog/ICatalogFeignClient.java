package com.vincentwang.ecommerce.order.client.catalog;

import com.vincentwang.ecommerce.order.client.catalog.dto.FeignProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service", url = "${order.catalog.service-url:}")
public interface ICatalogFeignClient {
    @GetMapping("/api/v1/products/{productId}")
    FeignProductResponse getProduct(@PathVariable("productId") long productId);
}
