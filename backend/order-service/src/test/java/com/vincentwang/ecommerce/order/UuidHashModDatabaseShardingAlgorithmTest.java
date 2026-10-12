package com.vincentwang.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincentwang.ecommerce.order.configuration.UuidHashModDatabaseShardingAlgorithm;
import java.util.List;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.junit.jupiter.api.Test;

class UuidHashModDatabaseShardingAlgorithmTest {

    private final UuidHashModDatabaseShardingAlgorithm algorithm = new UuidHashModDatabaseShardingAlgorithm();

    @Test
    void routesUuidStringsDeterministicallyAcrossTheTwoDatabases() {
        var even = new PreciseShardingValue<>("orders", "user_id", null, "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        var odd = new PreciseShardingValue<>("orders", "user_id", null, "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaab");

        assertThat(algorithm.doSharding(List.of("order_ds_1", "order_ds_0"), even))
                .isEqualTo("order_ds_0");
        assertThat(algorithm.doSharding(List.of("order_ds_1", "order_ds_0"), odd))
                .isEqualTo("order_ds_1");
    }
}
