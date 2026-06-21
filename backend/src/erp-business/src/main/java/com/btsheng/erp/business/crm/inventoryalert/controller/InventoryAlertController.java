package com.btsheng.erp.business.crm.inventoryalert.controller;

import com.btsheng.erp.business.crm.inventoryalert.dto.AlertResolveRequest;
import com.btsheng.erp.business.crm.inventoryalert.dto.InventoryAlertQueryRequest;
import com.btsheng.erp.business.crm.inventoryalert.dto.SafetyStockRequest;
import com.btsheng.erp.business.crm.inventoryalert.entity.CrmInventoryAlert;
import com.btsheng.erp.business.crm.inventoryalert.entity.CrmInventorySafety;
import com.btsheng.erp.business.crm.inventoryalert.service.InventoryAlertService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.14 · 安全库存与预警 Controller
 *
 * 3 端点：
 * - POST /inventory/safety                设置安全库存（AC-4.4.1）
 * - POST /inventory/check-alert           检查并触发预警（AC-4.4.2）
 * - GET  /inventory/alerts               预警列表
 */
@RestController
@RequestMapping("/inventory")
@Tag(name = "E4-Inventory-Alert", description = "安全库存与预警（Story 1.14）")
public class InventoryAlertController {

    private final InventoryAlertService alertService;

    @Autowired
    public InventoryAlertController(InventoryAlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/safety")
    @Operation(summary = "更新安全库存（AC-4.4.1）")
    public Result<CrmInventorySafety> updateSafetyConfig(
            @RequestBody SafetyStockRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return alertService.updateSafetyConfig(req, userId);
    }

    @GetMapping("/safety")
    @Operation(summary = "安全库存配置列表")
    public Result<List<CrmInventorySafety>> listSafetyConfigs() {
        return alertService.listSafetyConfigs();
    }

    @GetMapping("/safety/{materialCode}")
    @Operation(summary = "按物料编码获取安全库存")
    public Result<CrmInventorySafety> getSafetyConfig(@PathVariable String materialCode) {
        return alertService.getSafetyConfig(materialCode);
    }

    @PostMapping("/check-alert")
    @Operation(summary = "检查并触发预警（AC-4.4.2 · 4 级别）")
    public Result<CrmInventoryAlert> checkAlert(
            @RequestParam String materialCode,
            @RequestParam Integer currentQty) {
        return alertService.checkAlert(materialCode, currentQty);
    }

    @GetMapping("/alerts")
    @Operation(summary = "预警列表")
    public Result<Map<String, Object>> listAlerts(InventoryAlertQueryRequest query) {
        return alertService.listAlerts(query);
    }

    @PostMapping("/alerts/{alertId}/resolve")
    @Operation(summary = "解决预警（P1 修补 3 · 解决后归档）")
    public Result<CrmInventoryAlert> resolveAlert(
            @PathVariable Long alertId,
            @RequestBody AlertResolveRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return alertService.resolveAlert(alertId, req, userId);
    }

    @PostMapping("/alerts/{alertId}/archive")
    @Operation(summary = "归档预警")
    public Result<CrmInventoryAlert> archiveAlert(
            @PathVariable Long alertId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return alertService.archiveAlert(alertId, userId);
    }

    @GetMapping("/alerts/stats")
    @Operation(summary = "预警统计")
    public Result<List<Map<String, Object>>> alertStats() {
        return alertService.alertStats();
    }
}
