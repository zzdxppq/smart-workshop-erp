package com.btsheng.erp.core.config;

import com.btsheng.erp.core.web.DekLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;

/**
 * DEK 启动加载（erp-business / erp-platform 共用 · 扫描 erp-core 即可生效）
 */
@Configuration
public class DekBootstrapConfig implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(DekBootstrapConfig.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        String file = event.getEnvironment().getProperty("app.crypto.dek-file", "/etc/erp/dek.key");
        String dev = event.getEnvironment().getProperty("app.crypto.dek-dev", "");
        log.info("[DEK-Bootstrap] loading from file={} devFallbackConfigured={}", file, !dev.isEmpty());
        DekLoader.loadOrFallback(file, dev);
    }
}
