package com.vincentwang.ecommerce.catalog.search;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "catalog.search.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchSearchConfiguration {

    @Bean
    RestClient elasticsearchRestClient(@Value("${catalog.search.elasticsearch.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    ApplicationRunner initializeProductIndex(ElasticsearchProductSearchRepository searchClient) {
        return arguments -> searchClient.initializeIndex();
    }
}
