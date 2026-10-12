package com.vincentwang.ecommerce.gateway.ratelimit;

import static com.vincentwang.ecommerce.common.web.PlatformHttpHeaders.CORRELATION_ID;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Enforces one route policy and returns a consistent problem response when traffic is throttled. */
public class RateLimitGatewayFilter implements GatewayFilter {

    static final String RATE_LIMIT_STATUS_HEADER = "X-RateLimit-Status";
    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private final String policyId;
    private final RateLimiter<?> rateLimiter;
    private final KeyResolver keyResolver;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final boolean failOpen;

    public RateLimitGatewayFilter(
            String policyId,
            RateLimiter<?> rateLimiter,
            KeyResolver keyResolver,
            ObjectMapper objectMapper,
            boolean enabled,
            boolean failOpen) {
        this.policyId = policyId;
        this.rateLimiter = rateLimiter;
        this.keyResolver = keyResolver;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.failOpen = failOpen;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }
        Mono<RateLimitDecision> decision = keyResolver
                .resolve(exchange)
                .defaultIfEmpty("ip:unknown")
                .flatMap(key -> rateLimiter.isAllowed(policyId, key))
                .map(RateLimitDecision::available)
                .onErrorReturn(RateLimitDecision.redisUnavailable());
        return decision.flatMap(result -> result.unavailable()
                ? handleUnavailableLimiter(exchange, chain)
                : handleResponse(exchange, chain, result.response()));
    }

    private Mono<Void> handleResponse(
            ServerWebExchange exchange, GatewayFilterChain chain, RateLimiter.Response response) {
        response.getHeaders()
                .forEach((name, value) -> exchange.getResponse().getHeaders().set(name, value));
        if ("-1".equals(response.getHeaders().get(REMAINING_HEADER))) {
            return handleUnavailableLimiter(exchange, chain);
        }
        if (response.isAllowed()) {
            return chain.filter(exchange);
        }
        exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, "1");
        return writeProblem(
                exchange,
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded",
                "Too many requests for this client and route. Retry after one second.");
    }

    private Mono<Void> handleUnavailableLimiter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().getHeaders().set(RATE_LIMIT_STATUS_HEADER, "degraded");
        if (failOpen) {
            return chain.filter(exchange);
        }
        return writeProblem(
                exchange,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Rate limiter unavailable",
                "Request admission cannot be verified because the rate limiter is unavailable.");
    }

    private Mono<Void> writeProblem(ServerWebExchange exchange, HttpStatus status, String title, String detail) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        exchange.getResponse().getHeaders().setCacheControl("no-store");

        URI requestUri = exchange.getRequest().getURI();
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "https://ecommerce.local/problems/" + status.value());
        problem.put("title", title);
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("instance", requestUri.getRawPath());
        problem.put("correlationId", exchange.getRequest().getHeaders().getFirst(CORRELATION_ID));

        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(problem);
        } catch (JacksonException exception) {
            body = ("{\"title\":\"" + title + "\",\"status\":" + status.value() + "}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private record RateLimitDecision(RateLimiter.Response response, boolean unavailable) {

        private static RateLimitDecision available(RateLimiter.Response response) {
            return new RateLimitDecision(response, false);
        }

        private static RateLimitDecision redisUnavailable() {
            return new RateLimitDecision(null, true);
        }
    }
}
