package com.vincentwang.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincentwang.ecommerce.order.infrastructure.id.SnowflakeOrderIdGenerator;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SnowflakeOrderIdGeneratorTest {
    @Test
    void generatesUniqueIdsAcrossWorkers() {
        SnowflakeOrderIdGenerator firstInstance = new SnowflakeOrderIdGenerator(41);
        SnowflakeOrderIdGenerator secondInstance = new SnowflakeOrderIdGenerator(42);
        Set<Long> ids = new HashSet<>();

        for (int index = 0; index < 10_000; index++) {
            assertThat(ids.add(firstInstance.nextId())).isTrue();
            assertThat(ids.add(secondInstance.nextId())).isTrue();
        }

        assertThat(ids).hasSize(20_000);
    }
}
