package com.btsheng.erp.business.crm.planner.controller;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.planner.entity.CrmProcessAssignment;
import com.btsheng.erp.business.crm.planner.entity.CrmProductionNotification;
import com.btsheng.erp.business.crm.planner.entity.CrmProductionPlanning;
import com.btsheng.erp.business.crm.planner.service.ProductionPlanningService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.CurrentUserHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * V2.1 · 排产计划 Controller
 */
@Tag(name = "E3-Production-Planning", description = "排产计划管理（V2.1）")
@RestController
@RequestMapping("/production-planning")
public class ProductionPlanningController {

    private final ProductionPlanningService planningService;

    @Autowired
    public ProductionPlanningController(ProductionPlanningService planningService) {
        this.planningService = planningService;
    }

    @Operation(summary = "获取待转产订单列表（V2.1）")
    @GetMapping("/pending-orders")
    public Result<List<CrmOrder>> listPendingOrders() {
        return planningService.listPendingProductionOrders();
    }

    @Operation(summary = "获取订单排产详情（V2.1）")
    @GetMapping("/orders/{orderId}/detail")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable("orderId") Long orderId) {
        return planningService.getOrderPlanningDetail(orderId);
    }

    @Operation(summary = "创建排产计划（转工单第一步，V2.1）")
    @PostMapping("/orders/{orderId}/planning")
    public Result<CrmProductionPlanning> createPlanning(
            @PathVariable("orderId") Long orderId,
            @RequestParam(value = "plannerUserId", required = false) Long plannerUserId,
            @RequestParam(value = "plannerName", required = false) String plannerName) {
        Long userId = plannerUserId != null ? plannerUserId : CurrentUserHelper.currentUserId();
        return planningService.createPlanning(orderId, userId, plannerName);
    }

    @Operation(summary = "工序分配（V2.1）")
    @PostMapping("/{planningId}/assign")
    public Result<CrmProductionPlanning> assignProcesses(
            @PathVariable("planningId") Long planningId,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assignmentList = (List<Map<String, Object>>) request.get("assignments");
        List<CrmProcessAssignment> assignments = parseAssignments(assignmentList);
        Long userId = CurrentUserHelper.currentUserId();
        return planningService.assignProcesses(planningId, assignments, userId);
    }

    @Operation(summary = "排产确认（V2.1）")
    @PostMapping("/{planningId}/confirm")
    public Result<Map<String, Object>> confirmScheduling(
            @PathVariable("planningId") Long planningId,
            @RequestParam(value = "plannerUserId", required = false) Long plannerUserId) {
        Long userId = plannerUserId != null ? plannerUserId : CurrentUserHelper.currentUserId();
        return planningService.confirmScheduling(planningId, userId);
    }

    @Operation(summary = "发送工单通知（V2.1）")
    @PostMapping("/{planningId}/notify")
    public Result<List<CrmProductionNotification>> sendNotifications(
            @PathVariable("planningId") Long planningId,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> operatorUserIds = (List<Long>) request.get("operatorUserIds");
        return planningService.sendNotifications(planningId, operatorUserIds);
    }

    @Operation(summary = "获取排产计划详情（V2.1）")
    @GetMapping("/{planningId}")
    public Result<Map<String, Object>> getPlanningDetail(@PathVariable("planningId") Long planningId) {
        return planningService.getPlanningDetail(planningId);
    }

    @Operation(summary = "获取待排产计划列表（V2.1）")
    @GetMapping("/pending")
    public Result<List<CrmProductionPlanning>> listPendingPlannings() {
        return planningService.listPendingPlannings();
    }

    @Operation(summary = "计算预计完成日期（V2.1）")
    @GetMapping("/{planningId}/calculate-end")
    public Result<LocalDateTime> calculateEndDate(@PathVariable("planningId") Long planningId) {
        return planningService.calculateEndDate(planningId);
    }

    @SuppressWarnings("unchecked")
    private List<CrmProcessAssignment> parseAssignments(List<Map<String, Object>> assignmentList) {
        if (assignmentList == null) return null;
        return assignmentList.stream().map(a -> {
            CrmProcessAssignment assignment = new CrmProcessAssignment();
            assignment.setSequence((Integer) a.getOrDefault("sequence", 0));
            assignment.setProcessName((String) a.get("processName"));
            assignment.setMachineType((String) a.get("machineType"));
            if (a.get("machineId") != null) {
                assignment.setMachineId(Long.parseLong(a.get("machineId").toString()));
            }
            assignment.setMachineCode((String) a.get("machineCode"));
            if (a.get("operatorUserId") != null) {
                assignment.setOperatorUserId(Long.parseLong(a.get("operatorUserId").toString()));
            }
            assignment.setOperatorName((String) a.get("operatorName"));
            if (a.get("plannedStart") != null) {
                assignment.setPlannedStart(LocalDateTime.parse(a.get("plannedStart").toString()));
            }
            if (a.get("plannedEnd") != null) {
                assignment.setPlannedEnd(LocalDateTime.parse(a.get("plannedEnd").toString()));
            }
            assignment.setIsOutsource((Integer) a.getOrDefault("isOutsource", 0));
            if (a.get("outsourceVendorId") != null) {
                assignment.setOutsourceVendorId(Long.parseLong(a.get("outsourceVendorId").toString()));
            }
            assignment.setRemark((String) a.get("remark"));
            return assignment;
        }).toList();
    }
}
