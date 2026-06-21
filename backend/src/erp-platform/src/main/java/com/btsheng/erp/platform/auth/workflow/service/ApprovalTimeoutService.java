package com.btsheng.erp.platform.auth.workflow.service;

import com.btsheng.erp.core.infra.XxlJobBase;
import com.btsheng.erp.platform.auth.workflow.timeout.ApprovalTimeoutWatcher;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 审批超时调度 Service（V1.3.7 · Story 1.2 · T2.3 · AC-1.2.4）
 *
 * <p>注册到 xxl-job-admin（cron {@code 0 *&#47;30 * * * *} = 每 30 分钟一次）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Component
public class ApprovalTimeoutService extends XxlJobBase {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTimeoutService.class);

    private final ApprovalTimeoutWatcher watcher;

    @Autowired
    public ApprovalTimeoutService(ApprovalTimeoutWatcher watcher) {
        this.watcher = watcher;
    }

    /**
     * XXL-JOB 入口（V1.3.7 调度）。
     */
    @XxlJob("approvalTimeoutScan")
    @Override
    public void execute() {
        safeRun(() -> {
            int count = watcher.scanAndNotify();
            log.info("[XXL-JOB] approvalTimeoutScan 完成：推送 {} 条", count);
        });
    }
}
