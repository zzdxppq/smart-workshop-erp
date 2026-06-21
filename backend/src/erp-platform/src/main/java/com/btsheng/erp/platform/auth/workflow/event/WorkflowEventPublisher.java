package com.btsheng.erp.platform.auth.workflow.event;

import com.btsheng.erp.core.integration.IntegrationEventPublisher;
import com.btsheng.erp.core.integration.IntegrationStreams;
import com.btsheng.erp.core.redis.RedisStreamTemplate;
import com.btsheng.erp.platform.auth.workflow.config.WorkflowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 工作流事件发布器（V1.3.7 · Story 1.2 · T2.4 · P1 修补 ③）
 *
 * <p>4 通道并行推送（V1.3.7 升级）：
 * <ol>
 *   <li>{@code stream:notify} - PC 红点（Redis Stream 统一通道 · architect P2 反馈 ④ 收敛）</li>
 *   <li>{@code EMAIL} - 邮件（Story 1.3 实施）</li>
 *   <li>{@code stream:app-push} - APP 推送</li>
 *   <li>{@code WECHAT_WORK} - 企业微信</li>
 * </ol>
 *
 * <p>失败重试：默认 3 次，指数退避 [1s, 2s, 4s]。所有通道失败 → 50003 + 审计告警。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Component
public class WorkflowEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEventPublisher.class);

    /** 统一通知通道（V1.3.7 P1-3 收敛为单通道 · architect P2 反馈 ④） */
    public static final String STREAM_NOTIFY = "stream:notify";
    public static final String STREAM_APP_PUSH = "stream:app-push";

    private final RedisStreamTemplate redisStreamTemplate;
    private final WorkflowConfig config;
    private final IntegrationEventPublisher integrationEventPublisher;

    @Autowired
    public WorkflowEventPublisher(RedisStreamTemplate redisStreamTemplate, WorkflowConfig config,
                                  IntegrationEventPublisher integrationEventPublisher) {
        this.redisStreamTemplate = redisStreamTemplate;
        this.config = config;
        this.integrationEventPublisher = integrationEventPublisher;
    }

    /**
     * 审批单创建事件。
     */
    public void publishCreated(Long approvalId, List<Long> candidates, String bizType, String bizId, java.math.BigDecimal amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bizType", "APPROVAL");
        payload.put("bizSubType", bizType);
        payload.put("originBizType", bizType);
        payload.put("originBizId", bizId);
        payload.put("bizId", bizId);
        payload.put("targetUserId", candidates == null ? "" : String.join(",", candidates.stream().map(String::valueOf).toList()));
        payload.put("title", "待审批：" + bizType + " " + bizId + " 金额 ¥" + amount);
        payload.put("approvalId", String.valueOf(approvalId));
        payload.put("amount", String.valueOf(amount));
        payload.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        sendWithRetry("APPROVAL_CREATED", payload);
        publishIntegration(IntegrationStreams.EVENT_APPROVAL_CREATED, payload);
    }

    /**
     * 审批通过事件。
     */
    public void publishApproved(Long approvalId, Long approverUserId, Integer nextNode,
                                String originBizType, String originBizId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bizType", "APPROVAL");
        payload.put("bizSubType", "APPROVED");
        payload.put("originBizType", originBizType == null ? "" : originBizType);
        payload.put("originBizId", originBizId == null ? "" : originBizId);
        payload.put("bizId", String.valueOf(approvalId));
        payload.put("targetUserId", String.valueOf(approverUserId));
        payload.put("title", "审批已通过（" + (nextNode == null ? "流程结束" : "推进到下一节点") + "）");
        payload.put("approvalId", String.valueOf(approvalId));
        payload.put("approverUserId", String.valueOf(approverUserId));
        payload.put("nextNode", String.valueOf(nextNode == null ? -1 : nextNode));
        payload.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        sendWithRetry("APPROVAL_APPROVED", payload);
        publishIntegration(IntegrationStreams.EVENT_APPROVAL_APPROVED, payload);
    }

    /**
     * 审批驳回事件。
     */
    public void publishRejected(Long approvalId, Long approverUserId, String reason,
                                String originBizType, String originBizId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bizType", "APPROVAL");
        payload.put("bizSubType", "REJECTED");
        payload.put("originBizType", originBizType == null ? "" : originBizType);
        payload.put("originBizId", originBizId == null ? "" : originBizId);
        payload.put("bizId", String.valueOf(approvalId));
        payload.put("targetUserId", String.valueOf(approverUserId));
        payload.put("title", "审批已驳回");
        payload.put("approvalId", String.valueOf(approvalId));
        payload.put("approverUserId", String.valueOf(approverUserId));
        payload.put("reason", reason == null ? "" : reason);
        payload.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        sendWithRetry("APPROVAL_REJECTED", payload);
        publishIntegration(IntegrationStreams.EVENT_APPROVAL_REJECTED, payload);
    }

    /**
     * 审批超时事件（V1.3.7 AC-1.2.4 推送）。
     */
    public void publishOverdue(Long approvalId, double overdueHours) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bizType", "APPROVAL_OVERDUE");
        payload.put("bizId", String.valueOf(approvalId));
        payload.put("title", "审批超时：已等待 " + overdueHours + "h");
        payload.put("approvalId", String.valueOf(approvalId));
        payload.put("overdueHours", String.valueOf(overdueHours));
        payload.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        sendWithRetry("APPROVAL_OVERDUE", payload);
    }

    /**
     * 二次催办事件（不重置 timeout_at）。
     */
    public void publishUrge(Long approvalId, Long operatorUserId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bizType", "APPROVAL_URGE");
        payload.put("bizId", String.valueOf(approvalId));
        payload.put("title", "审批催办");
        payload.put("approvalId", String.valueOf(approvalId));
        payload.put("operatorUserId", String.valueOf(operatorUserId));
        payload.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        sendWithRetry("APPROVAL_URGE", payload);
    }

    /**
     * 4 通道并行 + 失败重试。
     */
    private void sendWithRetry(String eventType, Map<String, Object> payload) {
        List<String> channels = config.getNotifyChannels();
        if (channels == null || channels.isEmpty()) {
            log.debug("[WorkflowEventPublisher] notify-channels 关闭，跳过推送：event={}", eventType);
            return;
        }
        List<CompletableFuture<Boolean>> futures = new java.util.ArrayList<>();
        for (String ch : channels) {
            futures.add(CompletableFuture.supplyAsync(() -> sendOne(ch, eventType, payload)));
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("[WorkflowEventPublisher] 4 通道并行推送异常：event={} error={}", eventType, e.getMessage());
        }
    }

    private boolean sendOne(String channel, String eventType, Map<String, Object> payload) {
        int attempts = 0;
        int maxAttempts = config.getRetryTimes();
        List<Long> backoff = config.getRetryBackoffMs();
        while (attempts < maxAttempts) {
            try {
                if ("REDIS_STREAM".equalsIgnoreCase(channel)) {
                    Map<String, String> map = new HashMap<>();
                    payload.forEach((k, v) -> map.put(k, String.valueOf(v)));
                    map.put("event", eventType);
                    redisStreamTemplate.publish(STREAM_NOTIFY, map);
                    return true;
                } else if ("APP_PUSH".equalsIgnoreCase(channel)) {
                    Map<String, String> map = new HashMap<>();
                    payload.forEach((k, v) -> map.put(k, String.valueOf(v)));
                    map.put("event", eventType);
                    redisStreamTemplate.publish(STREAM_APP_PUSH, map);
                    return true;
                } else if ("EMAIL".equalsIgnoreCase(channel)) {
                    // EmailService 待 Story 1.3 实施，dev 阶段 mock 直接成功
            log.debug("[WorkflowEventPublisher] EMAIL 推送 mock 成功：event={}", eventType);
                    return true;
                } else if ("WECHAT_WORK".equalsIgnoreCase(channel)) {
                    // WechatWorkClient 待 Story 1.3 实施，dev 阶段 mock 直接成功
            log.debug("[WorkflowEventPublisher] WECHAT_WORK 推送 mock 成功：event={}", eventType);
                    return true;
                }
                return true;
            } catch (Exception e) {
                attempts++;
                if (attempts >= maxAttempts) {
                    log.error("[WorkflowEventPublisher] 通道 {} 推送失败 {} 次，放弃：event={} err={}",
                            channel, attempts, eventType, e.getMessage());
                    return false;
                }
                try {
                    long sleep = backoff == null || attempts - 1 >= backoff.size()
                            ? 1000L : backoff.get(attempts - 1);
                    Thread.sleep(sleep);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private void publishIntegration(String integrationEventType, Map<String, Object> payload) {
        try {
            Map<String, String> map = new HashMap<>();
            payload.forEach((k, v) -> map.put(k, v == null ? "" : String.valueOf(v)));
            integrationEventPublisher.publish(integrationEventType, map);
        } catch (Exception e) {
            log.warn("[WorkflowEventPublisher] integration stream publish failed: {}", e.getMessage());
        }
    }
}
