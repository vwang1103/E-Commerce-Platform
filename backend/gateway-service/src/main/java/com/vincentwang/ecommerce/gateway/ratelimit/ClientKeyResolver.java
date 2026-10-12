package com.vincentwang.ecommerce.gateway.ratelimit;

import com.vincentwang.ecommerce.common.web.PlatformHttpHeaders;
import com.vincentwang.ecommerce.gateway.configuration.GatewayRateLimitProperties;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class ClientKeyResolver implements KeyResolver {

    private static final Pattern VALID_USER_ID = Pattern.compile(PlatformHttpHeaders.UUID_PATTERN);
    private final boolean useForwardedFor;
    private final XForwardedRemoteAddressResolver forwardedAddressResolver;

    public ClientKeyResolver(GatewayRateLimitProperties properties) {
        this.useForwardedFor = properties.isUseForwardedFor();
        this.forwardedAddressResolver =
                XForwardedRemoteAddressResolver.maxTrustedIndex(properties.getTrustedProxyCount());
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst(PlatformHttpHeaders.USER_ID);
        if (userId != null && VALID_USER_ID.matcher(userId).matches()) {
            return Mono.just("user:" + userId);
        }

        InetSocketAddress remoteAddress = useForwardedFor
                ? forwardedAddressResolver.resolve(exchange)
                : exchange.getRequest().getRemoteAddress();
        return Mono.just("ip:" + normalizeAddress(remoteAddress));
    }

    private String normalizeAddress(InetSocketAddress address) {
        if (address == null) {
            return "unknown";
        }
        String host = address.getAddress() == null
                ? address.getHostString()
                : address.getAddress().getHostAddress();
        int zoneSeparator = host.indexOf('%');
        if (zoneSeparator >= 0) {
            host = host.substring(0, zoneSeparator);
        }
        return host.toLowerCase(Locale.ROOT);
    }
}
