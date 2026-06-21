package com.btsheng.erp.production.mrp.service;

import com.btsheng.erp.core.integration.IntegrationEventPublisher;
import com.btsheng.erp.core.integration.IntegrationStreams;
import com.btsheng.erp.production.mrp.dto.MrpRunRequest;
import com.btsheng.erp.production.mrp.dto.MrpRunResponse;
import com.btsheng.erp.core.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MRP 触发编排：事件 / 定时 / 手动 · 分布式锁防并发重复运算
 */
@Service
public class MrpTriggerService {

    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_EVENT = "EVENT";
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";

    private static final Logger log = LoggerFactory.getLogger(MrpTriggerService.class);
    private static final String LOCK_KEY = "mrp:run:lock";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    private final MrpService mrpService;
    private final StringRedisTemplate redis;
    private final IntegrationEventPublisher integrationEventPublisher;

    @Autowired
    public MrpTriggerService(MrpService mrpService,
                               StringRedisTemplate redis,
                               IntegrationEventPublisher integrationEventPublisher) {
        this.mrpService = mrpService;
        this.redis = redis;
        this.integrationEventPublisher = integrationEventPublisher;
    }

    /** 发布 MRP 触发事件（跨服务 / 异步消费） */
    public void publishTrigger(String source, Long userId) {
        Map<String, String> payload = new HashMap<>();
        payload.put("source", source != null ? source : "UNKNOWN");
        payload.put("userId", String.valueOf(userId != null ? userId : 1L));
        integrationEventPublisher.publish(IntegrationStreams.EVENT_MRP_TRIGGER, payload);
    }

    /** 带锁执行 MRP（手动 / 事件 / 定时统一入口） */
    public Result<MrpRunResponse> triggerRun(String triggerType, String source, MrpRunRequest req, Long userId) {
        String token = UUID.randomUUID().toString();
        Boolean locked = redis.opsForValue().setIfAbsent(LOCK_KEY, token, LOCK_TTL);
        if (Boolean.FALSE.equals(locked)) {
            log.info("[MrpTrigger] skip duplicate run trigger={} source={}", triggerType, source);
            return Result.fail(40901, "MRP_ALREADY_RUNNING");
        }
        try {
            MrpRunRequest runReq = req != null ? req : defaultRequest(triggerType);
            runReq.setTriggerType(triggerType);
            runReq.setTriggerSource(source);
            return mrpService.runMrp(runReq, userId);
        } finally {
            String current = redis.opsForValue().get(LOCK_KEY);
            if (token.equals(current)) {
                redis.delete(LOCK_KEY);
            }
        }
    }

    /** 消费集成事件时调用 */
    public void handleIntegrationEvent(Map<String, String> body) {
        String source = body.getOrDefault("source", "EVENT");
        Long userId = parseLong(body.get("userId"));
        Result<MrpRunResponse> r = triggerRun(TRIGGER_EVENT, source, defaultRequest(TRIGGER_EVENT), userId);
        if (!r.isSuccess()) {
            log.warn("[MrpTrigger] event run failed source={} msg={}", source, r.getMessage());
        }
    }

    private static MrpRunRequest defaultRequest(String triggerType) {
        MrpRunRequest req = new MrpRunRequest();
        LocalDate today = LocalDate.now();
        req.setDateRangeStart(today);
        req.setDateRangeEnd(today.plusDays(30));
        req.setWarehouseIds(List.of(1L, 2L, 3L));
        req.setRunType(TRIGGER_SCHEDULED.equals(triggerType) ? MrpService.RUN_TYPE_FULL : MrpService.RUN_TYPE_INCREMENTAL);
        return req;
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) return 1L;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 1L;
        }
    }
}
