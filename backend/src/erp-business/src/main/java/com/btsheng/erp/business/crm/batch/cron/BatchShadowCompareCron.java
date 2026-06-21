package com.btsheng.erp.business.crm.batch.cron;

import com.btsheng.erp.business.crm.batch.service.BatchService;
import com.btsheng.erp.core.infra.XxlJobBase;
import com.btsheng.erp.core.redis.RedisStreamTemplate;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * V1.3.8 · Story 3.1 · 影子表对比 cron job（AC-3.1.3）
 *
 * <p>V1.3.8 Sprint 7 集成 B：改用 XXL-JOB 体系（V1.3.7 既有架构）
 * <ul>
 *   <li>@XxlJob("batchShadowCompareHourly") 注册到 xxl-job-admin</li>
 *   <li>cron 表达式：0 0 * * * ?  （每小时整点）</li>
 *   <li>每 1h 执行一次，对比 crm_batch 与 crm_batch_shadow 聚合</li>
 *   <li>不一致率 > 0.1% 触发告警（architect review 3.1 §3.2 硬性约束）</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Component
public class BatchShadowCompareCron extends XxlJobBase {

    private static final Logger log = LoggerFactory.getLogger(BatchShadowCompareCron.class);

    private final BatchService batchService;
    private final RedisStreamTemplate redisStreamTemplate;

    @Autowired
    public BatchShadowCompareCron(BatchService batchService, RedisStreamTemplate redisStreamTemplate) {
        this.batchService = batchService;
        this.redisStreamTemplate = redisStreamTemplate;
    }

    /**
     * XXL-JOB 入口 · 每小时执行一次
     */
    @XxlJob("batchShadowCompareHourly")
    @Override
    public void execute() {
        safeRun(() -> {
            LocalDateTime sinceTime = LocalDateTime.now().minusHours(1);
            BatchService.ShadowComparison result = batchService.compareShadow(sinceTime);

            log.info("[BatchShadowCompareCron] hourly compare since={} total={} matched={} mismatched={} rate={} alert={}",
                    sinceTime, result.getTotal(), result.getMatched(),
                    result.getMismatched(), result.getMismatchRate(), result.isAlert());

            if (result.isAlert()) {
                Map<String, String> payload = new HashMap<>();
                payload.put("type", "BATCH_SHADOW_MISMATCH");
                payload.put("since", sinceTime.toString());
                payload.put("mismatchRate", String.valueOf(result.getMismatchRate()));
                payload.put("mismatched", String.valueOf(result.getMismatched()));
                payload.put("total", String.valueOf(result.getTotal()));
                redisStreamTemplate.publish("stream:notify", payload);
                log.warn("[BatchShadowCompareCron] ALERT: shadow mismatch rate {} > 0.1% threshold · stream:notify published",
                        result.getMismatchRate());
            }
        });
    }
}