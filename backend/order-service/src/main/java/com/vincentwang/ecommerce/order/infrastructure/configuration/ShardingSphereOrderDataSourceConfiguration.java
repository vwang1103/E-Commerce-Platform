package com.vincentwang.ecommerce.order.infrastructure.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ShardingSphereOrderDataSourceConfiguration {
    @Bean(destroyMethod = "close")
    @Primary
    DataSource orderDataSource(
            @Value("${order.datasource.shard-0.url}") String shard0Url,
            @Value("${order.datasource.shard-1.url}") String shard1Url,
            @Value("${order.datasource.username}") String username,
            @Value("${order.datasource.password}") String password)
            throws SQLException {
        Map<String, DataSource> dataSources = new LinkedHashMap<>();
        dataSources.put("order_ds_0", physicalDataSource("order-shard-0", shard0Url, username, password));
        dataSources.put("order_ds_1", physicalDataSource("order-shard-1", shard1Url, username, password));
        dataSources.values().forEach(this::migrate);

        Properties databaseHash = new Properties();
        databaseHash.setProperty("strategy", "STANDARD");
        databaseHash.setProperty("algorithmClassName", UuidHashModDatabaseShardingAlgorithm.class.getName());
        Properties orderTableInline = new Properties();
        orderTableInline.setProperty("algorithm-expression", "orders_${order_id % 2}");
        Properties itemTableInline = new Properties();
        itemTableInline.setProperty("algorithm-expression", "order_items_${order_id % 2}");

        ShardingRuleConfiguration rule = new ShardingRuleConfiguration();
        rule.setTables(List.of(
                shardedTable("orders", "order_ds_${0..1}.orders_${0..1}", "orders-inline"),
                shardedTable("order_items", "order_ds_${0..1}.order_items_${0..1}", "items-inline"),
                shardedTable("processed_event", "order_ds_${0..1}.processed_event", null),
                shardedTable("order_outbox", "order_ds_${0..1}.order_outbox", null)));
        rule.setShardingAlgorithms(Map.of(
                "database-hash", new AlgorithmConfiguration("CLASS_BASED", databaseHash),
                "orders-inline", new AlgorithmConfiguration("INLINE", orderTableInline),
                "items-inline", new AlgorithmConfiguration("INLINE", itemTableInline)));
        Properties props = new Properties();
        props.setProperty("sql-show", "false");
        Collection<RuleConfiguration> rules = List.of(rule);
        return ShardingSphereDataSourceFactory.createDataSource(dataSources, rules, props);
    }

    private static ShardingTableRuleConfiguration shardedTable(
            String logicTable, String actualDataNodes, String tableAlgorithm) {
        ShardingTableRuleConfiguration table = new ShardingTableRuleConfiguration(logicTable, actualDataNodes);
        table.setDatabaseShardingStrategy(new StandardShardingStrategyConfiguration("user_id", "database-hash"));
        if (tableAlgorithm != null) {
            table.setTableShardingStrategy(new StandardShardingStrategyConfiguration("order_id", tableAlgorithm));
        }
        return table;
    }

    private static HikariDataSource physicalDataSource(String poolName, String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        return new HikariDataSource(config);
    }

    private void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .validateMigrationNaming(true)
                .load()
                .migrate();
    }
}
