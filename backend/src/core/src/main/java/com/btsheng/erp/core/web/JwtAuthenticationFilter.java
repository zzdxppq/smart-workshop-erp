package com.btsheng.erp.core.web;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 鉴权过滤器（V1.3.9 · 绑定 DataScopeContext + SecurityContext）
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final RedisBlacklist blacklist;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, RedisBlacklist blacklist) {
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                authenticateBearer(header.substring(7).trim());
            }
            filterChain.doFilter(request, response);
        } finally {
            DataScopeContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateBearer(String token) {
        try {
            Claims claims = jwtUtil.parse(token);
            if (!JwtUtil.TYPE_ACCESS.equals(String.valueOf(claims.get(JwtUtil.CLAIM_TYPE)))) {
                return;
            }
            Long userId = claims.get(JwtUtil.CLAIM_USER_ID, Number.class).longValue();
            String username = claims.get(JwtUtil.CLAIM_USERNAME, String.class);
            if (blacklist.isBlacklisted(userId, claims.getId())) {
                return;
            }
            Long deptId = claims.get("deptId", Number.class) == null
                    ? 0L : claims.get("deptId", Number.class).longValue();
            String dataScope = claims.get("ds", String.class);
            String rolesCsv = claims.get(JwtUtil.CLAIM_ROLES, String.class);
            List<SimpleGrantedAuthority> authorities = parseAuthorities(rolesCsv);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(userId);
            SecurityContextHolder.getContext().setAuthentication(auth);
            DataScopeContext.bind(new DataScopeContext(userId, deptId, dataScope, "u", "d", false));
        } catch (JwtException ex) {
            log.debug("[JwtAuthenticationFilter] invalid token: {}", ex.getMessage());
        }
    }

    private static List<SimpleGrantedAuthority> parseAuthorities(String rolesCsv) {
        if (rolesCsv == null || rolesCsv.isBlank()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return Arrays.stream(rolesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
