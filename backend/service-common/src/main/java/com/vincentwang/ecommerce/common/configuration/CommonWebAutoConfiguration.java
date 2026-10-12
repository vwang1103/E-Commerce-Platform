package com.vincentwang.ecommerce.common.configuration;

import com.vincentwang.ecommerce.common.web.ServiceExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/** Gives every servlet-based service the same error responses without declaring a handler of its own. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ServiceExceptionHandler serviceExceptionHandler() {
        return new ServiceExceptionHandler();
    }
}
