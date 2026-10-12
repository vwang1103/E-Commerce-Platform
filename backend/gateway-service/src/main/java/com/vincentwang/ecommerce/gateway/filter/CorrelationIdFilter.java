package com.vincentwang.ecommerce.gateway.filter;

import static com.vincentwang.ecommerce.common.web.PlatformHttpHeaders.CORRELATION_ID;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Propagates a safe correlation ID through downstream services and back to the caller. */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Pattern VALID_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange);
        ServerWebExchange correlatedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(CORRELATION_ID, correlationId)))
                .build();
        correlatedExchange.getResponse().getHeaders().set(CORRELATION_ID, correlationId);
        return chain.filter(correlatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String resolveCorrelationId(ServerWebExchange exchange) {
        String supplied = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        if (supplied != null && VALID_CORRELATION_ID.matcher(supplied).matches()) {
            return supplied;
        }
        return UUID.randomUUID().toString();
    }
}
