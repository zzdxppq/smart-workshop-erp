package com.btsheng.erp.core.web;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具类（common-security · 复用给全部 11 个下游 Epic）
 *
 * <p>HS256 签名；access 2h + refresh 7d；jti UUID v4。
 * 解析失败抛 {@link JwtException}；过期抛 {@link ExpiredJwtException}。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Component
public class JwtUtil {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_USERNAME = "usr";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TYPE = "typ";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${app.security.jwt.secret:dev-only-secret-please-replace-in-prod-2026}")
    private String secret;

    @Value("${app.security.jwt.access-ttl-seconds:7200}")
    private long accessTtlSeconds;

    @Value("${app.security.jwt.refresh-ttl-seconds:604800}")
    private long refreshTtlSeconds;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String signAccessToken(Long userId, String username, String roles, Long deptId, String dataScope) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .claims(Map.of(
                        CLAIM_USER_ID, userId,
                        CLAIM_USERNAME, username,
                        CLAIM_TYPE, TYPE_ACCESS,
                        CLAIM_ROLES, roles == null ? "" : roles,
                        "deptId", deptId == null ? 0L : deptId,
                        "ds", dataScope == null ? "SELF" : dataScope
                ))
                .signWith(key())
                .compact();
    }

    public String signRefreshToken(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claims(Map.of(
                        CLAIM_USER_ID, userId,
                        CLAIM_USERNAME, username,
                        CLAIM_TYPE, TYPE_REFRESH
                ))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public long remainingSeconds(String token) {
        try {
            Claims c = parse(token);
            return Math.max((c.getExpiration().getTime() - System.currentTimeMillis()) / 1000L, 0);
        } catch (ExpiredJwtException e) {
            return 0;
        }
    }
}
