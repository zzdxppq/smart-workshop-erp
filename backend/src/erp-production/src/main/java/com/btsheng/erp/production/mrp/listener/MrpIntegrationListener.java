package com.btsheng.erp.production.mrp.listener;

import com.btsheng.erp.core.integration.IntegrationStreamPoller;
import com.btsheng.erp.core.integration.IntegrationStreams;
import com.btsheng.erp.core.redis.RedisStreamTemplate;
import com.btsheng.erp.production.mrp.service.MrpTriggerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 集成事件 → MRP 自动重算（工序分配 / 销售审批等）
 */
@Component
public class MrpIntegrationListener extends IntegrationStreamPoller {

    private static final String GROUP = "erp-production-mrp";
    private static final String CONSUMER = "production-mrp-1";

    private final MrpTriggerService mrpTriggerService;

    public MrpIntegrationListener(RedisStreamTemplate redisStreamTemplate,
                                  MrpTriggerService mrpTriggerService) {
        super(redisStreamTemplate, GROUP, CONSUMER);
        this.mrpTriggerService = mrpTriggerService;
    }

    @Scheduled(fixedDelayString = "${app.integration.poll-ms:3000}")
    public void pollEvents() {
        poll(20, record -> {
            var body = record.getValue();
            String eventType = field(body, "eventType");
            if (IntegrationStreams.EVENT_MRP_TRIGGER.equals(eventType)) {
                mrpTriggerService.handleIntegrationEvent(body);
            }
        });
    }
}
