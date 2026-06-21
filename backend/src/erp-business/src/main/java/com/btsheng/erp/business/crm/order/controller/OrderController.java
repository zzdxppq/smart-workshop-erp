package com.btsheng.erp.business.crm.order.controller;

import com.btsheng.erp.business.crm.order.dto.OrderCancelRequest;
import com.btsheng.erp.business.crm.order.dto.OrderConfirmRequest;
import com.btsheng.erp.business.crm.order.dto.OrderCreateRequest;
import com.btsheng.erp.business.crm.order.dto.OrderUpdateRequest;
import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.service.OrderPdfExportService;
import com.btsheng.erp.business.crm.order.service.OrderProfitService;
import com.btsheng.erp.business.crm.order.service.OrderService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.CurrentUserHelper;
import com.btsheng.erp.core.web.SalesDataScopeHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.6 · 订单 8 端点 Controller
 *
 * <p>AC-2.3.1 CRUD (POST/GET/PUT/list) + AC-2.3.2 状态机 (confirm/cancel)
 * <br>+ AC-2.3.4 转下游 (ship) + 导出 (export)
 */
@Tag(name = "E3-Order", description = "订单管理")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderPdfExportService exportService;
    private final OrderProfitService profitService;

    @Autowired
    public OrderController(OrderService orderService, OrderPdfExportService exportService,
                           OrderProfitService profitService) {
        this.orderService = orderService;
        this.exportService = exportService;
        this.profitService = profitService;
    }

    // CRUD (AC-2.3.1)
            @Operation(summary = "创建订单")
    @PostMapping
    public Result<CrmOrder> create(@RequestBody OrderCreateRequest req) {
        return orderService.createOrder(req.getOrder(), req.getItems(), 1L);
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable("id") Long id) {
        Result<Map<String, Object>> detail = orderService.getOrderWithDetail(id);
        if (!detail.isSuccess() || detail.getData() == null) {
            return detail;
        }
        if (SalesDataScopeHelper.effectiveScope() != SalesDataScopeHelper.Scope.ALL) {
            Object o = detail.getData().get("order");
            if (o instanceof CrmOrder order) {
                Result<Void> scope = SalesDataScopeHelper.assertOwnerDept(order.getOwnerUserId(), order.getDeptId());
                if (!scope.isSuccess()) {
                    return Result.fail(scope.getCode(), scope.getMessage());
                }
            }
        }
        return detail;
    }

    @Operation(summary = "订单时间线（E2-S3）")
    @GetMapping("/{id}/timeline")
    public Result<List<Map<String, Object>>> timeline(@PathVariable("id") Long id) {
        Result<CrmOrder> scoped = requireOrderInScope(id);
        if (!scoped.isSuccess()) {
            return Result.fail(scoped.getCode(), scoped.getMessage());
        }
        return orderService.getOrderTimeline(id);
    }

    @Operation(summary = "修改订单（仅 DRAFT 状态）")
    @PutMapping("/{id}")
    public Result<CrmOrder> update(@PathVariable("id") Long id, @RequestBody OrderUpdateRequest req) {
        return orderService.updateOrder(id, req.getOrder(), req.getItems(), 1L);
    }

    @Operation(summary = "保存订单草稿（含明细行 · V2.1）")
    @PutMapping("/{id}/draft")
    public Result<CrmOrder> saveDraft(@PathVariable("id") Long id, @RequestBody OrderCreateRequest req) {
        return orderService.saveDraftWithItems(id, req.getOrder(), req.getItems(), 1L);
    }

    @Operation(summary = "列表查询（按权限过滤）")
    @GetMapping
    public Result<List<CrmOrder>> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                       @RequestParam(value = "size", defaultValue = "20") int size,
                                       @RequestParam(value = "status", required = false) String status,
                                       @RequestParam(value = "customerId", required = false) Long customerId,
                                       @RequestParam(value = "owner", required = false) Long owner,
                                       @RequestParam(value = "role", defaultValue = "gm") String role,
                                       @RequestParam(value = "deptId", required = false) Long deptId) {
        Long scopedOwner = SalesDataScopeHelper.resolveOwnerUserId(owner);
        Long scopedDept = SalesDataScopeHelper.resolveDeptId(deptId);
        String scopedRole = SalesDataScopeHelper.resolveListRole(role);
        return orderService.listOrders(page, size, status, customerId, scopedOwner, scopedDept, scopedRole);
    }

    private Result<CrmOrder> requireOrderInScope(Long id) {
        Result<CrmOrder> r = orderService.getOrder(id);
        if (!r.isSuccess() || r.getData() == null) {
            return r;
        }
        if (SalesDataScopeHelper.effectiveScope() != SalesDataScopeHelper.Scope.ALL) {
            CrmOrder order = r.getData();
            Result<Void> scope = SalesDataScopeHelper.assertOwnerDept(order.getOwnerUserId(), order.getDeptId());
            if (!scope.isSuccess()) {
                return Result.fail(scope.getCode(), scope.getMessage());
            }
        }
        return r;
    }

    // 状态机 (AC-2.3.2)
            @Operation(summary = "确认订单（DRAFT → CONFIRMED）")
    @PostMapping("/{id}/confirm")
    public Result<CrmOrder> confirm(@PathVariable("id") Long id, @RequestBody(required = false) OrderConfirmRequest req) {
        return orderService.confirmOrder(id, 1L);
    }

    @Operation(summary = "审批通过（CONFIRMED → PRODUCING）")
    @PostMapping("/{id}/approve")
    public Result<CrmOrder> approve(@PathVariable("id") Long id) {
        return orderService.approveOrder(id, 1L);
    }

    @Operation(summary = "驳回（CONFIRMED → DRAFT）")
    @PostMapping("/{id}/reject")
    public Result<CrmOrder> reject(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        return orderService.rejectOrder(id, body.getOrDefault("reason", "未说明"), 1L);
    }

    @Operation(summary = "取消订单（→ CANCELLED）")
    @PostMapping("/{id}/cancel")
    public Result<CrmOrder> cancel(@PathVariable("id") Long id, @RequestBody OrderCancelRequest req) {
        return orderService.cancelOrder(id, req.getReason(), 1L);
    }

    // ========== V2.1 新增端点 ==========

    @Operation(summary = "提交订单（V2.1 · DRAFT → APPROVED，无需审批，自动生成料号）")
    @PostMapping("/{id}/submit")
    public Result<Map<String, Object>> submitOrder(@PathVariable("id") Long id) {
        return orderService.submitOrder(id, 1L);
    }

    @Operation(summary = "批量检查图号是否有已有料号（V2.1）")
    @GetMapping("/check-material-nos")
    public Result<Map<String, String>> checkMaterialNos(@RequestParam("drawingNos") List<String> drawingNos) {
        return orderService.checkMaterialNos(drawingNos);
    }

    // 转下游 (AC-2.3.4)
            @Operation(summary = "转生产（CONFIRMED → PRODUCING，Feign 创建 GD 工单 · AC-5.1.1）")
    @PostMapping("/{id}/convert-to-production")
    public Result<Map<String, Object>> convertToProduction(@PathVariable("id") Long id) {
        return orderService.startProduction(id, 1L);
    }

    @Operation(summary = "待转产订单（CONFIRMED 且无工单号）")
    @GetMapping("/pending-production")
    public Result<Map<String, Object>> listPendingProduction(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return orderService.listPendingProduction(pageNum, pageSize);
    }

    @Operation(summary = "转委外（生成 WW 单号）")
    @PostMapping("/{id}/convert-to-outsource")
    public Result<CrmOrder> convertToOutsource(@PathVariable("id") Long id) {
        return orderService.transferToOutsource(id, 1L);
    }

    @Operation(summary = "发货（PRODUCING → SHIPPED/PARTIAL_SHIPPED）")
    @PostMapping("/{id}/ship")
    public Result<CrmOrder> ship(@PathVariable("id") Long id, @RequestBody(required = false) Map<Long, Integer> itemShipMap) {
        if (itemShipMap == null || itemShipMap.isEmpty()) {
            return orderService.ship(id, 1L);
        }
        return orderService.partialShip(id, itemShipMap, 1L);
    }

    @Operation(summary = "结算（SHIPPED → SETTLED）")
    @PostMapping("/{id}/settle")
    public Result<CrmOrder> settle(@PathVariable("id") Long id) {
        return orderService.settle(id, 1L);
    }

    @Operation(summary = "关闭（SETTLED → CLOSED 终态）")
    @PostMapping("/{id}/close")
    public Result<CrmOrder> close(@PathVariable("id") Long id) {
        return orderService.closeOrder(id, 1L);
    }

    // 导出 (AC-2.3.4)
            @Operation(summary = "导出 PDF/Excel")
    @GetMapping("/export/{id}")
    public Result<byte[]> export(@PathVariable("id") Long id,
                                 @RequestParam(value = "format", defaultValue = "pdf") String format) {
        if ("excel".equalsIgnoreCase(format)) return exportService.exportExcel(id, 1L);
        return exportService.exportPdf(id, 1L);
    }

    @Operation(summary = "利润分析")
    @GetMapping("/{id}/profit")
    public Result<Map<String, Object>> profit(@PathVariable("id") Long id) {
        Result<CrmOrder> scoped = requireOrderInScope(id);
        if (!scoped.isSuccess()) {
            return Result.fail(scoped.getCode(), scoped.getMessage());
        }
        return profitService.analyzeProfit(id, SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    @Operation(summary = "利润分析 PDF")
    @GetMapping("/{id}/profit/export")
    public Result<byte[]> profitPdf(@PathVariable("id") Long id) {
        Result<CrmOrder> scoped = requireOrderInScope(id);
        if (!scoped.isSuccess()) {
            return Result.fail(scoped.getCode(), scoped.getMessage());
        }
        return profitService.exportProfitPdf(id, SalesDataScopeHelper.requireOperatorUserId(1L));
    }
}
