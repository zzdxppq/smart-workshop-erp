package com.btsheng.erp.core.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁（V1.3.7 · 简化 SET NX EX 模式）
 *
 * <p>基于 Redis 单实例 SET key value NX EX ttl 实现。自动生成 token，释放时 Lua 脚本校验。
 * 高可用（Redlock）由 Story 1.3 实装。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Component
public class DistributedLock {

    private static final String UNLOCK_LUA = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private final StringRedisTemplate redis;

    @Autowired
    public DistributedLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public <T> T tryLock(String key, long ttlSeconds, Supplier<T> action) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redis.execute((RedisCallback<Boolean>) conn -> conn.stringCommands().set(
                key.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8),
                Expiration.from(ttlSeconds, TimeUnit.SECONDS),
                RedisStringCommands.SetOption.SET_IF_ABSENT));
        if (Boolean.TRUE.equals(ok)) {
            try {
                return action.get();
            } finally {
                redis.execute((RedisCallback<Long>) conn -> conn.scriptingCommands().eval(
                        UNLOCK_LUA.getBytes(StandardCharsets.UTF_8),
                        org.springframework.data.redis.connection.ReturnType.INTEGER,
                        1,
                        key.getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8)));
            }
        }
        return null;
    }
}
