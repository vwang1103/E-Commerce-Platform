package com.vincentwang.ecommerce.gateway.filter;

import static com.vincentwang.ecommerce.common.web.PlatformHttpHeaders.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesAValidCorrelationIdForDownstreamAndResponse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products")
                .header(CORRELATION_ID, "request-123")
                .build());
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, filtered -> {
                    downstream.set(filtered);
                    return filtered.getResponse().setComplete();
                })
                .block();

        assertThat(downstream.get().getRequest().getHeaders().getFirst(CORRELATION_ID))
                .isEqualTo("request-123");
        assertThat(exchange.getResponse().getHeaders().getFirst(CORRELATION_ID)).isEqualTo("request-123");
    }

    @Test
    void replacesAnInvalidCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products")
                .header(CORRELATION_ID, "invalid id with spaces")
                .build());
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, filtered -> {
                    downstream.set(filtered);
                    return filtered.getResponse().setComplete();
                })
                .block();

        String correlationId = downstream.get().getRequest().getHeaders().getFirst(CORRELATION_ID);
        assertThat(correlationId).isNotBlank().doesNotContain(" ");
        assertThat(exchange.getResponse().getHeaders().getFirst(CORRELATION_ID)).isEqualTo(correlationId);
    }
}
