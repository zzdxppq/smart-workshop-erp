package com.btsheng.erp.core.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security 配置（V1.3.7）
 *
 * <p>无状态 + 白名单（{@code /auth/login} / {@code /health/**}）+ {@code @PreAuthorize} 注解授权。
 * 全部 11 个下游服务复用此配置，差异在白名单。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        // V1.3.7 强制 cost=12
            return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   InternalApiAuthFilter internalApiAuthFilter,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(internalApiAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/auth/login",
                        "/auth/refresh",
                        "/platform/health/**",
                        "/platform/files/preview/**",
                        "/ws",
                        "/ws/**",
                        "/sse/**",
                        "/actuator/health/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**"
                ).permitAll()
                .requestMatchers("/internal/**").hasRole("INTERNAL")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );
        return http.build();
    }
}
