package com.btsheng.erp.platform.auth.security;

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
 * JWT 签发器（V1.3.7 · BR-4）
 *
 * <p>HS256 签名；access 2h + refresh 7d；jti 唯一（UUID v4）。
 * 黑名单 TTL = token 剩余有效期（V1.3.7 红线）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Component
public class JwtSigner {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_USERNAME = "usr";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_DEPT_ID = "deptId";
    public static final String CLAIM_DATA_SCOPE = "ds";

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

    /**
     * 签发 access token。
     */
    public String signAccessToken(Long userId, String username, String roles, Long deptId, String dataScope) {
        return sign(userId, username, roles, deptId, dataScope, TYPE_ACCESS, accessTtlSeconds);
    }

    public String signRefreshToken(Long userId, String username) {
        return sign(userId, username, null, null, null, TYPE_REFRESH, refreshTtlSeconds);
    }

    private String sign(Long userId, String username, String roles, Long deptId, String dataScope,
                        String type, long ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttl)))
                .claims(Map.of(
                        CLAIM_USER_ID, userId,
                        CLAIM_USERNAME, username,
                        CLAIM_TYPE, type,
                        CLAIM_ROLES, roles == null ? "" : roles,
                        CLAIM_DEPT_ID, deptId == null ? 0L : deptId,
                        CLAIM_DATA_SCOPE, dataScope == null ? "SELF" : dataScope
                ))
                .signWith(key())
                .compact();
    }

    /**
     * 解析 token；过期/篡改抛 {@link JwtException}。
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    /**
     * 计算 token 剩余有效秒数（用于 Redis 黑名单 TTL = 剩余有效期）。
     */
    public long remainingSeconds(String token) {
        try {
            Claims claims = parse(token);
            Date exp = claims.getExpiration();
            long remaining = (exp.getTime() - System.currentTimeMillis()) / 1000L;
            return Math.max(remaining, 0);
        } catch (ExpiredJwtException e) {
            return 0;
        }
    }
}
