package com.btsheng.erp.business.realtime;

import com.btsheng.erp.business.crm.dashboard.outsource.service.OutsourceDashboardService;
import com.btsheng.erp.business.crm.dashboard.production.service.ProductionDashboardService;
import com.btsheng.erp.core.model.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 5s 广播 dashboard / schedule 频道（WS Spec B.2） */
@Component
public class RealtimeBroadcastScheduler {

    private final RealtimeSessionRegistry registry;
    private final ProductionDashboardService dashboardService;
    private final OutsourceDashboardService outsourceDashboardService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public RealtimeBroadcastScheduler(RealtimeSessionRegistry registry,
                                      ProductionDashboardService dashboardService,
                                      OutsourceDashboardService outsourceDashboardService) {
        this.registry = registry;
        this.dashboardService = dashboardService;
        this.outsourceDashboardService = outsourceDashboardService;
    }

    @Scheduled(fixedRate = 5000)
    public void pushDashboardFeeds() {
        pushJson("dashboard:kpi", safeData(dashboardService.getKpiFlat()));
        pushJson("dashboard:kanban", safeData(dashboardService.getKanbanFeed(50)));
        pushJson("dashboard:events", safeData(dashboardService.getEventsFeed(20)));
        pushJson("dashboard:outsource", safeData(outsourceDashboardService.getOverview(50)));
        // 排产数据已迁移至 erp-production · schedule 频道暂由 production 服务推送
            pushJson("schedule:machine", Map.of("jobs", java.util.List.of()));
        pushJson("approval:new", Map.of("count", 1, "title", "新审批待办"));
        pushJson("message:new", Map.of("count", 1, "title", "新消息"));
        pushJson("scan:progress", Map.of("status", "OK", "ts", System.currentTimeMillis()));
    }

    @Scheduled(fixedRate = 600000)
    public void pushSseChannels() {
        pushJson("inventory:alert", Map.of("level", "WARN", "message", "库存低于安全线"));
        pushJson("payment:remind", Map.of("level", "INFO", "message", "回款提醒"));
    }

    private void pushJson(String channel, Object payload) {
        try {
            registry.broadcast(channel, objectMapper.writeValueAsString(payload));
        } catch (Exception ignored) {
        }
    }

    private Object safeData(Result<?> result) {
        if (result != null && result.isSuccess() && result.getData() != null) {
            return result.getData();
        }
        return Map.of();
    }
}
