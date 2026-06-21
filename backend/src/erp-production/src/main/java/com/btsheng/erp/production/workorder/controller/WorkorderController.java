package com.btsheng.erp.production.workorder.controller;

import com.btsheng.erp.production.workorder.dto.CurrentProcessResponse;
import com.btsheng.erp.production.workorder.dto.WorkorderCreateRequest;
import com.btsheng.erp.production.workorder.dto.WorkorderQueryRequest;
import com.btsheng.erp.production.workorder.dto.WorkorderScheduleRequest;
import com.btsheng.erp.production.workorder.dto.WorkorderTimelineResponse;
import com.btsheng.erp.production.workorder.entity.CrmProductionSchedule;
import com.btsheng.erp.production.workorder.entity.CrmWorkorder;
import com.btsheng.erp.production.workorder.entity.CrmWorkorderStep;
import com.btsheng.erp.production.workorder.service.WorkorderService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.15 · 工单与排产 Controller
 *
 * 5 端点：
 * - POST /workorders                创建工单（AC-5.1.1）
 * - PUT  /workorders/{id}/schedule  排产（AC-5.1.2）
 * - POST /workorders/{id}/start     开工
 * - POST /workorders/{id}/finish    完工
 * - GET  /workorders                列表
 */
@RestController
@RequestMapping("/workorders")
@Tag(name = "E5-Workorder", description = "工单与排产（Story 1.15）")
public class WorkorderController {

    private final WorkorderService workorderService;

    @Autowired
    public WorkorderController(WorkorderService workorderService) {
        this.workorderService = workorderService;
    }

    @PostMapping
    @Operation(summary = "创建工单（AC-5.1.1）")
    public Result<CrmWorkorder> createWorkorder(
            @RequestBody WorkorderCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return workorderService.createWorkorder(req, userId);
    }

    @PostMapping("/from-order")
    @Operation(summary = "销售订单转工单（AC-5.1.1 · Feign）")
    public Result<Map<String, Object>> createFromOrder(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return workorderService.createWorkorderFromOrder(mapToCreateRequest(body), userId);
    }

    @GetMapping("/by-no/{workorderNo}")
    @Operation(summary = "按工单号查询")
    public Result<Map<String, Object>> getByNo(@PathVariable String workorderNo) {
        return workorderService.getWorkorderByNo(workorderNo);
    }

    @GetMapping("/by-sales-order/{orderId}")
    @Operation(summary = "按销售订单 ID 查询工单")
    public Result<Map<String, Object>> getBySalesOrderId(@PathVariable Long orderId) {
        return workorderService.getWorkorderBySalesOrderId(orderId);
    }

    @GetMapping("/visitor-search")
    @Operation(summary = "客户现场演示 · 脱敏工单搜索（V1.4.0）")
    public Result<Map<String, Object>> visitorSearch(@RequestParam("keyword") String keyword) {
        return workorderService.visitorSearch(keyword);
    }

    @GetMapping("/visitor-active")
    @Operation(summary = "客户现场演示 · 默认活跃工单列表（脱敏，无 keyword）")
    public Result<Map<String, Object>> visitorActive(
            @RequestParam(value = "limit", defaultValue = "23") Integer limit) {
        return workorderService.visitorActive(limit);
    }

    @GetMapping("/visitor-detail/{workorderNo}")
    @Operation(summary = "客户现场演示 · 单工单详情 + 工序时间线（脱敏）")
    public Result<Map<String, Object>> visitorDetail(@PathVariable String workorderNo) {
        return workorderService.visitorDetail(workorderNo);
    }

    private WorkorderCreateRequest mapToCreateRequest(Map<String, Object> body) {
        WorkorderCreateRequest req = new WorkorderCreateRequest();
        if (body == null) return req;
        req.setSalesOrderId(asLong(body.get("salesOrderId")));
        req.setSalesOrderNo(asString(body.get("salesOrderNo")));
        req.setMaterialCode(asString(body.get("materialCode")));
        req.setProductName(asString(body.get("productName")));
        req.setQty(asInt(body.get("qty")));
        req.setDrawingId(asLong(body.get("drawingId")));
        req.setBomId(asLong(body.get("bomId")));
        req.setProcessRouteId(asLong(body.get("processRouteId")));
        req.setWorkorderNo(asString(body.get("workorderNo")));
        req.setDeliveryDate(asString(body.get("deliveryDate")));
        req.setPriority(asInt(body.get("priority")));
        req.setIsFa(asInt(body.get("isFa")));
        return req;
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (NumberFormatException e) { return null; }
    }

    private static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (NumberFormatException e) { return null; }
    }

    @PutMapping("/{id}/schedule")
    @Operation(summary = "工单排产（AC-5.1.2 · 冲突检测）")
    public Result<CrmProductionSchedule> scheduleWorkorder(
            @PathVariable Long id,
            @RequestBody WorkorderScheduleRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return workorderService.scheduleWorkorder(id, req, userId);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "开工")
    public Result<CrmWorkorder> startProduction(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return workorderService.startProduction(id, userId);
    }

    @PostMapping("/{id}/finish")
    @Operation(summary = "完工")
    public Result<CrmWorkorder> finishProduction(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return workorderService.finishProduction(id, userId);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消工单")
    public Result<CrmWorkorder> cancelWorkorder(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return workorderService.cancelWorkorder(id, userId);
    }

    @GetMapping
    @Operation(summary = "工单列表")
    public Result<Map<String, Object>> listWorkorders(WorkorderQueryRequest query) {
        return workorderService.listWorkorders(query);
    }

    @GetMapping("/current-process")
    @Operation(summary = "操作工当前工序（Story 12.1 · 5min Redis 缓存）")
    public Result<CurrentProcessResponse> getCurrentProcess(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return workorderService.getCurrentProcess(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "工单详情")
    public Result<CrmWorkorder> getWorkorder(@PathVariable Long id) {
        return workorderService.getWorkorder(id);
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "工单 timeline（AC-5.1.3）")
    public Result<WorkorderTimelineResponse> getTimeline(@PathVariable Long id) {
        return workorderService.getTimeline(id);
    }

    @GetMapping("/{id}/steps")
    @Operation(summary = "工单工序列表")
    public Result<List<CrmWorkorderStep>> listSteps(@PathVariable Long id) {
        return workorderService.listSteps(id);
    }

    @GetMapping("/schedules")
    @Operation(summary = "排产列表")
    public Result<List<CrmProductionSchedule>> listSchedules() {
        return workorderService.listSchedules();
    }
}
