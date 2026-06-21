package com.btsheng.erp.core.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 通用缓存模板（V1.3.7）
 *
 * <p>读穿透：cache miss → loader 加载 → 回写 Redis。TTL 默认 3600s。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Component
public class CacheTemplate {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public CacheTemplate(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public <T> T getOrLoad(String key, TypeReference<T> typeRef, long ttlSeconds, Supplier<T> loader) {
        String json = redis.opsForValue().get(key);
        if (json != null) {
            try {
                return mapper.readValue(json, typeRef);
            } catch (Exception e) {
                // 缓存损坏，回源
            }
        }
        T value = loader.get();
        if (value != null) {
            try {
                redis.opsForValue().set(key, mapper.writeValueAsString(value), Duration.ofSeconds(ttlSeconds));
            } catch (Exception ignored) {
            }
        }
        return value;
    }

    public void evict(String key) {
        redis.delete(key);
    }

    public void evictByPattern(String pattern) {
        redis.delete(Collections.emptySet());
        // 简化：实际可用 KEYS 或 SCAN；为避免阻塞 redis-server 在 KEYS 命令，此处保留接口签名
    }

    public boolean setIfAbsent(String key, String value, long ttlSeconds) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS));
    }
}
