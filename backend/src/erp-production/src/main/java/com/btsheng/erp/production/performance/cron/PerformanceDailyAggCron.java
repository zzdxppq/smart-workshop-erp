package com.btsheng.erp.production.performance.cron;

import com.btsheng.erp.core.infra.XxlJobBase;
import com.btsheng.erp.production.performance.service.PerformanceDailyAggService;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * V1.4.0 · E11-S6 · 绩效日聚合 XXL-JOB
 *
 * <p>注册名：performanceDailyAgg
 * <p>建议 cron：0 30 0 * * ?（每日 00:30）
 */
@Component
public class PerformanceDailyAggCron extends XxlJobBase {

    private static final Logger log = LoggerFactory.getLogger(PerformanceDailyAggCron.class);

    private final PerformanceDailyAggService aggService;

    @Autowired
    public PerformanceDailyAggCron(PerformanceDailyAggService aggService) {
        this.aggService = aggService;
    }

    @XxlJob("performanceDailyAgg")
    @Override
    public void execute() {
        safeRun(() -> {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            int rows = aggService.aggregateForDate(yesterday);
            log.info("[PerformanceDailyAggCron] statDate={} rows={}", yesterday, rows);
        });
    }
}
