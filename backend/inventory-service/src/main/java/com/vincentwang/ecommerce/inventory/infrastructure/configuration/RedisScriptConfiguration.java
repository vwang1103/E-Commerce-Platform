package com.vincentwang.ecommerce.inventory.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisScriptConfiguration {

    @Bean
    RedisScript<Long> inventoryTryScript() {
        return script("redis/inventory-try.lua");
    }

    @Bean
    RedisScript<Long> inventoryConfirmScript() {
        return script("redis/inventory-confirm.lua");
    }

    @Bean
    RedisScript<Long> inventoryCancelScript() {
        return script("redis/inventory-cancel.lua");
    }

    private static RedisScript<Long> script(String location) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(location));
        script.setResultType(Long.class);
        return script;
    }
}
