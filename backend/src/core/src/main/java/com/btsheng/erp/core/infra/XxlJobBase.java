package com.btsheng.erp.core.infra;

/**
 * XXL-JOB 基类占位（V1.3.7）
 *
 * <p>统一异常处理 / 日志格式 / 监控埋点。完整实装在 Story 1.3。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public abstract class XxlJobBase {

    /** 业务执行入口 */
    public abstract void execute() throws Exception;

    /** 异常统一包装 */
    protected void safeRun(Runnable body) {
        try {
            body.run();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("[XXL-JOB] failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
