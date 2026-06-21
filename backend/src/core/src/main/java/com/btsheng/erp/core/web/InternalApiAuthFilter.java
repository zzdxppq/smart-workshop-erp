package com.btsheng.erp.core.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 微服务内部 API 鉴权：{@code /internal/**} 需携带 {@code X-Internal-Token}（生产环境必填）。
 */
@Component
public class InternalApiAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalApiAuthFilter.class);
    public static final String HEADER = "X-Internal-Token";
    public static final String ROLE_INTERNAL = "ROLE_INTERNAL";

    @Value("${app.internal.token:}")
    private String expectedToken;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (expectedToken == null || expectedToken.isBlank()) {
            log.debug("[InternalApi] token not configured, allowing /internal in dev");
            setInternalAuth();
            filterChain.doFilter(request, response);
            return;
        }
        String token = request.getHeader(HEADER);
        if (token == null || !expectedToken.equals(token.trim())) {
            log.warn("[InternalApi] invalid token for {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        setInternalAuth();
        filterChain.doFilter(request, response);
    }

    private static void setInternalAuth() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "internal-service", null,
                List.of(new SimpleGrantedAuthority(ROLE_INTERNAL)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
