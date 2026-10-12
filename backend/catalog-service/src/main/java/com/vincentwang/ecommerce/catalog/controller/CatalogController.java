package com.vincentwang.ecommerce.catalog.controller;

import com.vincentwang.ecommerce.catalog.controller.dto.request.CreateProductRequest;
import com.vincentwang.ecommerce.catalog.controller.dto.request.UpdateProductRequest;
import com.vincentwang.ecommerce.catalog.controller.dto.response.CategoryResponse;
import com.vincentwang.ecommerce.catalog.controller.dto.response.ProductResponse;
import com.vincentwang.ecommerce.catalog.controller.dto.response.ProductSearchResponse;
import com.vincentwang.ecommerce.catalog.service.ICatalogService;
import com.vincentwang.ecommerce.common.web.ApiPageResponse;
import com.vincentwang.ecommerce.common.web.PlatformHttpHeaders;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Public HTTP entry point for browsing and seller-managed catalog operations. */
@Validated
@RestController
@RequestMapping
public class CatalogController {

    private final ICatalogService catalogService;

    public CatalogController(ICatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/api/v1/categories")
    public List<CategoryResponse> categories() {
        return catalogService.listCategories();
    }

    @PostMapping("/api/v1/products")
    public ResponseEntity<ProductResponse> create(
            @RequestHeader(PlatformHttpHeaders.USER_ID) @Pattern(regexp = PlatformHttpHeaders.UUID_PATTERN) String userId,
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = catalogService.createProduct(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + product.productId()))
                .body(product);
    }

    @GetMapping("/api/v1/products/{productId}")
    public ProductResponse get(@PathVariable @Positive long productId) {
        return catalogService.getProduct(productId);
    }

    @GetMapping("/api/v1/products")
    public ApiPageResponse<ProductResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = catalogService.listProducts(null, page, size);
        return ApiPageResponse.of(result.items(), result.page(), result.size(), result.totalElements());
    }

    @GetMapping("/api/v1/products/mine")
    public ApiPageResponse<ProductResponse> listMine(
            @RequestHeader(PlatformHttpHeaders.USER_ID) @Pattern(regexp = PlatformHttpHeaders.UUID_PATTERN) String userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = catalogService.listProducts(userId, page, size);
        return ApiPageResponse.of(result.items(), result.page(), result.size(), result.totalElements());
    }

    @PutMapping("/api/v1/products/{productId}")
    public ProductResponse update(
            @RequestHeader(PlatformHttpHeaders.USER_ID) @Pattern(regexp = PlatformHttpHeaders.UUID_PATTERN) String userId,
            @PathVariable @Positive long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return catalogService.updateProduct(userId, productId, request);
    }

    @GetMapping("/api/v1/search")
    public ProductSearchResponse search(
            @RequestParam("q") @NotBlank @Size(max = 100) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ProductSearchResponse.from(catalogService
                .searchProducts(query.trim(), page, size)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Elasticsearch search is disabled")));
    }

    @GetMapping("/internal/benchmarks/search/mysql")
    public ProductSearchResponse benchmarkMySqlSearch(
            @RequestParam("q") @NotBlank @Size(max = 100) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ProductSearchResponse.from(catalogService
                .benchmarkMySqlSearch(query.trim(), page, size)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MySQL search benchmark is disabled")));
    }
}
