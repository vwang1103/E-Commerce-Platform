package com.vincentwang.ecommerce.order.infrastructure.configuration;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

/** Routes the same UUID string to the same order database by its stable Java hash. */
public final class UuidHashModDatabaseShardingAlgorithm implements StandardShardingAlgorithm<String> {

    @Override
    public String doSharding(Collection<String> availableTargets, PreciseShardingValue<String> shardingValue) {
        List<String> targets = sortedTargets(availableTargets);
        int index = Math.floorMod(shardingValue.getValue().hashCode(), targets.size());
        return targets.get(index);
    }

    @Override
    public Collection<String> doSharding(
            Collection<String> availableTargets, RangeShardingValue<String> shardingValue) {
        return sortedTargets(availableTargets);
    }

    private static List<String> sortedTargets(Collection<String> availableTargets) {
        if (availableTargets.isEmpty()) {
            throw new IllegalArgumentException("At least one order database target is required");
        }
        return availableTargets.stream().sorted(Comparator.naturalOrder()).toList();
    }
}
