package com.btsheng.erp.platform.print.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 打印异步线程池（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.1 · TC-12.4.1.8）
 *
 * <p>独立线程池防 Socket 阻塞占满 HTTP 线程
 * <p>配置：core=4 · max=16 · queue=200 · CallerRunsPolicy（队列满时由调用者线程执行，避免任务丢失）
 *
 * <p>异步异常处理：{@link PrintAsyncExceptionHandler} · 写 sys_print_log.status=FAILED 兜底
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Configuration
@EnableAsync
public class PrintAsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(PrintAsyncConfig.class);

    @Bean(name = "printZplExecutor")
    public Executor printZplExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("print-zpl-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 允许核心线程超时回收
            executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        log.info("[PrintAsyncConfig] printZplExecutor initialized: core=4 max=16 queue=200 CallerRuns");
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new PrintAsyncExceptionHandler();
    }
}
