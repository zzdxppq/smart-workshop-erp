package com.btsheng.erp.production.integration.config;

import com.btsheng.erp.core.web.InternalApiAuthFilter;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Feign 调用 erp-business /internal/** 时自动附带 {@link InternalApiAuthFilter#HEADER}。 */
@Configuration
public class InternalFeignConfig {

    @Bean
    public RequestInterceptor internalTokenFeignInterceptor(
            @Value("${app.internal.token:}") String token) {
        return template -> {
            if (token != null && !token.isBlank()) {
                template.header(InternalApiAuthFilter.HEADER, token);
            }
        };
    }
}
