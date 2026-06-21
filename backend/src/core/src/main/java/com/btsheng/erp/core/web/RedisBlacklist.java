package com.btsheng.erp.core.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT 黑名单（V1.3.7 · BR-4 · 关键红线）
 *
 * <p><b>TTL 语义</b>：key TTL = access_token 剩余有效期（秒），严禁固定 2h。
 * 用户禁用 / 改密 / 登出 5 分钟内踢出。
 *
 * <p>Key 命名空间：{@code auth:blacklist:{userId}}
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Component
public class RedisBlacklist {

    public static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redis;

    @Autowired
    public RedisBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 写入黑名单，TTL = token 剩余有效期（秒）。
     *
     * @param userId          用户 ID
     * @param jti             JWT ID
     * @param remainingSeconds token 剩余 TTL（秒），必须 > 0
     */
    public void blacklist(Long userId, String jti, long remainingSeconds) {
        if (userId == null || jti == null) {
            return;
        }
        if (remainingSeconds <= 0) {
            return; // 已过期，无需写入
        }
        redis.opsForValue().set(buildKey(userId, jti), "1", Duration.ofSeconds(remainingSeconds));
    }

    public boolean isBlacklisted(Long userId, String jti) {
        if (userId == null || jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(buildKey(userId, jti)));
    }

    /**
     * 用户维度踢出（"修改密码 / 禁用账户"场景）。
     * 写入 sentinel key，所有 token 在 5 分钟内被拒。
     */
    public void evictUser(Long userId, long ttlSeconds) {
        if (userId == null) return;
        redis.opsForValue().set("auth:evict:" + userId, String.valueOf(System.currentTimeMillis()),
                Duration.ofSeconds(ttlSeconds));
    }

    public boolean isUserEvicted(Long userId, Long tokenIssuedAtMillis) {
        if (userId == null) return false;
        String v = redis.opsForValue().get("auth:evict:" + userId);
        if (v == null) return false;
        long evictAt = Long.parseLong(v);
        return tokenIssuedAtMillis == null || tokenIssuedAtMillis < evictAt;
    }

    private String buildKey(Long userId, String jti) {
        return KEY_PREFIX + userId + ":" + jti;
    }
}
