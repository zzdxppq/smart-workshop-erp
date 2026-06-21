package com.btsheng.erp.business.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 将当前用户 JWT 转发到 erp-platform 等下游 Feign 调用。 */
@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            String auth = attrs.getRequest().getHeader("Authorization");
            if (auth != null && !auth.isBlank()) {
                template.header("Authorization", auth);
            }
        };
    }
}
