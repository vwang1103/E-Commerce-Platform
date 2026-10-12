package com.vincentwang.ecommerce.gateway.configuration;

import com.vincentwang.ecommerce.gateway.ratelimit.ClientKeyResolver;
import com.vincentwang.ecommerce.gateway.ratelimit.RateLimitGatewayFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayRateLimitProperties.class)
public class GatewayRouteConfiguration {

    @Bean
    ClientKeyResolver clientKeyResolver(GatewayRateLimitProperties properties) {
        return new ClientKeyResolver(properties);
    }

    @Bean("generalRateLimiter")
    @Primary
    RedisRateLimiter generalRateLimiter(GatewayRateLimitProperties properties) {
        return createRateLimiter(properties.getGeneral());
    }

    @Bean("searchRateLimiter")
    RedisRateLimiter searchRateLimiter(GatewayRateLimitProperties properties) {
        return createRateLimiter(properties.getSearch());
    }

    @Bean("orderWriteRateLimiter")
    RedisRateLimiter orderWriteRateLimiter(GatewayRateLimitProperties properties) {
        return createRateLimiter(properties.getOrderWrite());
    }

    @Bean
    RouteLocator gatewayRoutes(
            RouteLocatorBuilder builder,
            ClientKeyResolver keyResolver,
            ObjectMapper objectMapper,
            GatewayRateLimitProperties properties,
            @Qualifier("generalRateLimiter") RedisRateLimiter generalRateLimiter,
            @Qualifier("searchRateLimiter") RedisRateLimiter searchRateLimiter,
            @Qualifier("orderWriteRateLimiter") RedisRateLimiter orderWriteRateLimiter) {
        GatewayFilter catalogRateLimit =
                rateLimit("catalog-api", generalRateLimiter, keyResolver, objectMapper, properties);
        GatewayFilter searchRateLimit =
                rateLimit("catalog-search", searchRateLimiter, keyResolver, objectMapper, properties);
        GatewayFilter inventoryRateLimit =
                rateLimit("inventory-api", generalRateLimiter, keyResolver, objectMapper, properties);
        GatewayFilter orderReadRateLimit =
                rateLimit("order-read", generalRateLimiter, keyResolver, objectMapper, properties);
        GatewayFilter orderWriteRateLimit =
                rateLimit("order-write", orderWriteRateLimiter, keyResolver, objectMapper, properties);

        return builder.routes()
                .route("catalog-search", route -> route.path("/api/v1/search", "/api/v1/search/**")
                        .filters(filters -> filters.filter(searchRateLimit))
                        .uri("lb://catalog-service"))
                .route("order-submit", route -> route.path("/api/v1/orders")
                        .and()
                        .method(HttpMethod.POST)
                        .filters(filters -> filters.filter(orderWriteRateLimit))
                        .uri("lb://order-service"))
                .route("order-payment", route -> route.path("/api/v1/orders/*/payment")
                        .and()
                        .method(HttpMethod.POST)
                        .filters(filters -> filters.filter(orderWriteRateLimit))
                        .uri("lb://order-service"))
                .route("catalog-api", route -> route.path(
                                "/api/v1/products", "/api/v1/products/**", "/api/v1/categories")
                        .filters(filters -> filters.filter(catalogRateLimit))
                        .uri("lb://catalog-service"))
                .route("inventory-api", route -> route.path("/api/v1/inventory", "/api/v1/inventory/**")
                        .filters(filters -> filters.filter(inventoryRateLimit))
                        .uri("lb://inventory-service"))
                .route("order-read", route -> route.path("/api/v1/orders/**")
                        .filters(filters -> filters.filter(orderReadRateLimit))
                        .uri("lb://order-service"))
                .build();
    }

    private RedisRateLimiter createRateLimiter(GatewayRateLimitProperties.Policy policy) {
        return new RedisRateLimiter(policy.getReplenishRate(), policy.getBurstCapacity());
    }

    private GatewayFilter rateLimit(
            String policyId,
            RedisRateLimiter rateLimiter,
            ClientKeyResolver keyResolver,
            ObjectMapper objectMapper,
            GatewayRateLimitProperties properties) {
        return new RateLimitGatewayFilter(
                policyId, rateLimiter, keyResolver, objectMapper, properties.isEnabled(), properties.isFailOpen());
    }
}
