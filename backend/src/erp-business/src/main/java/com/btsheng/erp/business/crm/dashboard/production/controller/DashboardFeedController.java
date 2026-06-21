package com.btsheng.erp.business.crm.dashboard.production.controller;

import com.btsheng.erp.business.crm.dashboard.multidim.service.DashboardGmService;
import com.btsheng.erp.business.crm.dashboard.production.service.ProductionDashboardService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Spec B.2 · dashboard:kanban / dashboard:events HTTP 轮询端点 */
@Tag(name = "E11-Dashboard-Feed", description = "生产工作台实时 Feed")
@RestController
@RequestMapping("/dashboard")
public class DashboardFeedController {

    private final ProductionDashboardService productionDashboardService;
    private final DashboardGmService gmService;

    @Autowired
    public DashboardFeedController(ProductionDashboardService productionDashboardService,
                                   DashboardGmService gmService) {
        this.productionDashboardService = productionDashboardService;
        this.gmService = gmService;
    }

    @Operation(summary = "看板数据（WS dashboard:kanban 轮询兜底）")
    @GetMapping("/kanban")
    public Result<Map<String, Object>> kanban(@RequestParam(defaultValue = "50") int limit) {
        return productionDashboardService.getKanbanFeed(limit);
    }

    @Operation(summary = "异常事件流（WS dashboard:events 轮询兜底）")
    @GetMapping("/events")
    public Result<Map<String, Object>> events(@RequestParam(defaultValue = "20") int limit) {
        return productionDashboardService.getEventsFeed(limit);
    }

    @GetMapping("/index")
    @Operation(summary = "工作台首页汇总")
    public Result<Map<String, Object>> index() {
        return gmService.getIndexSummary();
    }
}
