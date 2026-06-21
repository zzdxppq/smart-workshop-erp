package com.btsheng.erp.business.integration.listener;

import com.btsheng.erp.business.crm.workflowevent.service.WorkflowEventService;
import com.btsheng.erp.core.integration.IntegrationStreamPoller;
import com.btsheng.erp.core.integration.IntegrationEventPublisher;
import com.btsheng.erp.core.integration.IntegrationStreams;
import com.btsheng.erp.core.redis.RedisStreamTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 审批集成事件消费 · platform {@code stream:integration} → business 本地审计
 */
@Component
public class ApprovalIntegrationListener extends IntegrationStreamPoller {

    private static final Logger log = LoggerFactory.getLogger(ApprovalIntegrationListener.class);
    private static final String GROUP = "erp-business-approval";
    private static final String CONSUMER = "business-approval-1";

    private final WorkflowEventService workflowEventService;
    private final IntegrationEventPublisher integrationEventPublisher;

    public ApprovalIntegrationListener(RedisStreamTemplate redisStreamTemplate,
                                       WorkflowEventService workflowEventService,
                                       IntegrationEventPublisher integrationEventPublisher) {
        super(redisStreamTemplate, GROUP, CONSUMER);
        this.workflowEventService = workflowEventService;
        this.integrationEventPublisher = integrationEventPublisher;
    }

    @Scheduled(fixedDelayString = "${app.integration.poll-ms:3000}")
    public void pollEvents() {
        poll(20, this::handle);
    }

    private void handle(org.springframework.data.redis.connection.stream.MapRecord<String, String, String> record) {
        Map<String, String> body = record.getValue();
        String eventType = field(body, "eventType");
        if (eventType == null) {
            return;
        }
        String originBizType = field(body, "originBizType");
        if (originBizType == null || originBizType.isBlank()) {
            originBizType = field(body, "bizSubType");
        }
        String originBizId = field(body, "originBizId");
        if (originBizId == null || originBizId.isBlank()) {
            originBizId = field(body, "bizId");
        }
        Long approvalId = parseLong(field(body, "approvalId"));
        Long approverUserId = parseLong(field(body, "approverUserId"));

        switch (eventType) {
            case IntegrationStreams.EVENT_APPROVAL_CREATED -> workflowEventService.recordEvent(
                    field(body, "workflowCode") != null ? field(body, "workflowCode") : originBizType + "_FLOW",
                    approvalId,
                    originBizId,
                    WorkflowEventService.EVENT_CREATED,
                    null,
                    approverUserId,
                    null,
                    null,
                    null,
                    field(body, "amount"));
            case IntegrationStreams.EVENT_APPROVAL_APPROVED -> {
                workflowEventService.recordEvent(
                    originBizType + "_FLOW",
                    approvalId,
                    originBizId,
                    WorkflowEventService.EVENT_APPROVED,
                    null,
                    approverUserId,
                    null,
                    field(body, "comment"),
                    parseInt(field(body, "nextNode")),
                    null);
                if (isSalesOrderApproval(originBizType)) {
                    Map<String, String> mrpPayload = new java.util.HashMap<>();
                    mrpPayload.put("source", "SALES_ORDER_APPROVED:" + originBizId);
                    mrpPayload.put("userId", String.valueOf(approverUserId != null ? approverUserId : 1L));
                    integrationEventPublisher.publish(IntegrationStreams.EVENT_MRP_TRIGGER, mrpPayload);
                }
            }
            case IntegrationStreams.EVENT_APPROVAL_REJECTED -> workflowEventService.recordEvent(
                    originBizType + "_FLOW",
                    approvalId,
                    originBizId,
                    WorkflowEventService.EVENT_REJECTED,
                    null,
                    approverUserId,
                    null,
                    field(body, "reason"),
                    null,
                    null);
            default -> { /* 非审批事件 */ }
        }
        log.debug("[ApprovalIntegrationListener] handled event={} approvalId={}", eventType, approvalId);
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isSalesOrderApproval(String originBizType) {
        if (originBizType == null) return false;
        String t = originBizType.toUpperCase();
        return t.contains("SALES_ORDER") || t.contains("ORDER") && t.contains("SALES");
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
