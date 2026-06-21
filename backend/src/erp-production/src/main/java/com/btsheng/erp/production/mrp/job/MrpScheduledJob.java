package com.btsheng.erp.production.mrp.job;

import com.btsheng.erp.production.mrp.service.MrpTriggerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日 7:00 自动 MRP 运算（覆盖夜间变动）
 */
@Component
public class MrpScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(MrpScheduledJob.class);

    private final MrpTriggerService mrpTriggerService;

    public MrpScheduledJob(MrpTriggerService mrpTriggerService) {
        this.mrpTriggerService = mrpTriggerService;
    }

    @Scheduled(cron = "${app.mrp.scheduled-cron:0 0 7 * * ?}")
    public void dailyMrpRun() {
        log.info("[MrpScheduledJob] daily MRP run starting");
        var result = mrpTriggerService.triggerRun(
                MrpTriggerService.TRIGGER_SCHEDULED,
                "CRON_0700",
                null,
                1L);
        if (result.isSuccess()) {
            log.info("[MrpScheduledJob] completed runNo={}", result.getData() != null ? result.getData().getRunNo() : "—");
        } else {
            log.warn("[MrpScheduledJob] failed: {}", result.getMessage());
        }
    }
}
