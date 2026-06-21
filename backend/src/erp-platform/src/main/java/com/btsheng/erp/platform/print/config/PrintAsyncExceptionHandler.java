package com.btsheng.erp.platform.print.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

/**
 * 打印异步异常处理（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.1 · TC-12.4.1.7）
 *
 * <p>@Async 线程内异常统一捕获 · 写 ERROR 日志 + 调用方自行补 FAILED log
 * <p>防静默吞异常导致审计漏失（architect R4 重点）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
public class PrintAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PrintAsyncExceptionHandler.class);

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("[PrintAsyncExceptionHandler] uncaught in {}: {}",
                method.getName(), ex.getMessage(), ex);
        // 调用方（PrintService.sendZpl）已在外层 try-catch 写 FAILED log
        // 此处仅日志兜底 · 防止异常被 Spring 静默吞掉
    }
}
