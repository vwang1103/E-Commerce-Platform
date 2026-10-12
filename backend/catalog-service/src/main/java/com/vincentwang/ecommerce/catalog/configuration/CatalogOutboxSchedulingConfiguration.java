package com.vincentwang.ecommerce.catalog.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "catalog.events", name = "enabled", havingValue = "true")
public class CatalogOutboxSchedulingConfiguration {}
