package com.vincentwang.ecommerce.gateway.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincentwang.ecommerce.gateway.ratelimit.ClientKeyResolver;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webflux.autoconfigure.WebFluxProperties;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.handler.predicate.MethodRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.support.GenericApplicationContext;
import tools.jackson.databind.ObjectMapper;

class GatewayRouteConfigurationTest {

    @Test
    void declaresOnlyTheExpectedPublicServiceRoutes() {
        GatewayRouteConfiguration configuration = new GatewayRouteConfiguration();
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        ClientKeyResolver keyResolver = configuration.clientKeyResolver(properties);
        RedisRateLimiter general = configuration.generalRateLimiter(properties);
        RedisRateLimiter search = configuration.searchRateLimiter(properties);
        RedisRateLimiter orderWrite = configuration.orderWriteRateLimiter(properties);
        RouteLocator routeLocator;

        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(WebFluxProperties.class);
            context.registerBean(PathRoutePredicateFactory.class);
            context.registerBean(MethodRoutePredicateFactory.class);
            context.refresh();
            routeLocator = configuration.gatewayRoutes(
                    new RouteLocatorBuilder(context),
                    keyResolver,
                    new ObjectMapper(),
                    properties,
                    general,
                    search,
                    orderWrite);
            Map<String, URI> routes = routeLocator
                    .getRoutes()
                    .collectMap(route -> route.getId(), route -> route.getUri())
                    .block();

            assertThat(routes)
                    .containsExactlyInAnyOrderEntriesOf(Map.of(
                            "catalog-search", URI.create("lb://catalog-service"),
                            "catalog-api", URI.create("lb://catalog-service"),
                            "inventory-api", URI.create("lb://inventory-service"),
                            "order-submit", URI.create("lb://order-service"),
                            "order-payment", URI.create("lb://order-service"),
                            "order-read", URI.create("lb://order-service")));
            assertThat(routes).doesNotContainKey("internal-benchmarks");
        }
    }
}
