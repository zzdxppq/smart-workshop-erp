package com.btsheng.erp.core.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 不可用时降级为直查 DB，避免报表/详情接口 500。
 */
@Configuration
public class ErpCacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ErpCacheConfig.class);

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[Cache] get failed cache={} key={}: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("[Cache] put failed cache={} key={}: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[Cache] evict failed cache={} key={}: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("[Cache] clear failed cache={}: {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
