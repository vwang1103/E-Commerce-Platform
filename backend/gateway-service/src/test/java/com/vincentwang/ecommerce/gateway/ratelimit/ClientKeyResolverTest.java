package com.vincentwang.ecommerce.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class ClientKeyResolverTest {

    private static final String USER_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";

    @Test
    void usesAValidatedUserIdBeforeTheNetworkAddress() {
        ClientKeyResolver resolver = new ClientKeyResolver(new GatewayRateLimitProperties());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-User-Id", USER_ID)
                .remoteAddress(new InetSocketAddress("127.0.0.1", 50123))
                .build());

        assertThat(resolver.resolve(exchange).block()).isEqualTo("user:" + USER_ID);
    }

    @Test
    void ignoresAnInvalidUserIdAndUsesTheRemoteAddress() {
        ClientKeyResolver resolver = new ClientKeyResolver(new GatewayRateLimitProperties());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-User-Id", "not-a-user")
                .remoteAddress(new InetSocketAddress("192.0.2.10", 50123))
                .build());

        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:192.0.2.10");
    }

    @Test
    void trustsOnlyTheConfiguredNumberOfForwardedProxies() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setUseForwardedFor(true);
        properties.setTrustedProxyCount(1);
        ClientKeyResolver resolver = new ClientKeyResolver(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "198.51.100.25")
                .remoteAddress(new InetSocketAddress("10.0.0.10", 443))
                .build());

        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:198.51.100.25");
    }
}
