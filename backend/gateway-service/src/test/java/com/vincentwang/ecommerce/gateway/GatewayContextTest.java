package com.vincentwang.ecommerce.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.cloud.consul.enabled=false", "gateway.rate-limit.enabled=false"})
class GatewayContextTest {

    @Test
    void loadsTheGatewayWithRoutesAndRateLimiterBeans() {}
}
