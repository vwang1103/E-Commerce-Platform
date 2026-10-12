package com.vincentwang.ecommerce.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

class RateLimitGatewayFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void forwardsAnAllowedRequestAndCopiesRateLimitHeaders() {
        RateLimiter.Response allowed = new RateLimiter.Response(
                true,
                Map.of(
                        "X-RateLimit-Remaining", "9",
                        "X-RateLimit-Burst-Capacity", "10"));
        AtomicBoolean chainCalled = new AtomicBoolean();
        MockServerWebExchange exchange = exchange();

        filter(allowed, true)
                .filter(exchange, filtered -> {
                    chainCalled.set(true);
                    return filtered.getResponse().setComplete();
                })
                .block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"))
                .isEqualTo("9");
    }

    @Test
    void rejectsAnExceededRequestWithAProblemResponse() {
        RateLimiter.Response rejected = new RateLimiter.Response(
                false,
                Map.of(
                        "X-RateLimit-Remaining", "0",
                        "X-RateLimit-Burst-Capacity", "10"));
        AtomicBoolean chainCalled = new AtomicBoolean();
        MockServerWebExchange exchange = exchange();

        filter(rejected, true)
                .filter(exchange, filtered -> {
                    chainCalled.set(true);
                    return Mono.empty();
                })
                .block();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("1");
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("Rate limit exceeded", "correlation-123", "/api/v1/orders");
    }

    @Test
    void marksRedisFailureAsDegradedAndFailsOpenWhenConfigured() {
        RateLimiter.Response unavailable = new RateLimiter.Response(true, Map.of("X-RateLimit-Remaining", "-1"));
        AtomicBoolean chainCalled = new AtomicBoolean();
        MockServerWebExchange exchange = exchange();

        filter(unavailable, true)
                .filter(exchange, filtered -> {
                    chainCalled.set(true);
                    return filtered.getResponse().setComplete();
                })
                .block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getHeaders().getFirst(RateLimitGatewayFilter.RATE_LIMIT_STATUS_HEADER))
                .isEqualTo("degraded");
    }

    @Test
    void returnsServiceUnavailableWhenFailClosedIsConfigured() {
        RateLimiter.Response unavailable = new RateLimiter.Response(true, Map.of("X-RateLimit-Remaining", "-1"));
        AtomicBoolean chainCalled = new AtomicBoolean();
        MockServerWebExchange exchange = exchange();

        filter(unavailable, false)
                .filter(exchange, filtered -> {
                    chainCalled.set(true);
                    return Mono.empty();
                })
                .block();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("Rate limiter unavailable");
    }

    @Test
    void doesNotConvertOrReplayADownstreamFailure() {
        RateLimiter.Response allowed = new RateLimiter.Response(true, Map.of("X-RateLimit-Remaining", "9"));
        AtomicBoolean chainCalled = new AtomicBoolean();
        MockServerWebExchange exchange = exchange();

        Mono<Void> result = filter(allowed, true).filter(exchange, filtered -> {
            if (!chainCalled.compareAndSet(false, true)) {
                throw new AssertionError("The downstream chain was invoked more than once");
            }
            return Mono.error(new IllegalStateException("downstream failed"));
        });

        assertThatThrownBy(result::block)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("downstream failed");
        assertThat(chainCalled).isTrue();
    }

    private RateLimitGatewayFilter filter(RateLimiter.Response response, boolean failOpen) {
        return new RateLimitGatewayFilter(
                "order-write",
                new TestRateLimiter(response),
                exchange -> Mono.just("user:42"),
                objectMapper,
                true,
                failOpen);
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/orders")
                .header("X-Correlation-Id", "correlation-123")
                .build());
    }

    private static final class TestRateLimiter implements RateLimiter<Object> {

        private final Response response;

        private TestRateLimiter(Response response) {
            this.response = response;
        }

        @Override
        public Mono<Response> isAllowed(String routeId, String id) {
            return Mono.just(response);
        }

        @Override
        public Map<String, Object> getConfig() {
            return Map.of();
        }

        @Override
        public Class<Object> getConfigClass() {
            return Object.class;
        }

        @Override
        public Object newConfig() {
            return new Object();
        }
    }
}
