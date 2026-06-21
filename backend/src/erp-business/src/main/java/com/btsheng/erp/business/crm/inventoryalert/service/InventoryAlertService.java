package com.btsheng.erp.business.crm.inventoryalert.service;

import com.btsheng.erp.business.crm.inventoryalert.dto.AlertResolveRequest;
import com.btsheng.erp.business.crm.inventoryalert.dto.InventoryAlertQueryRequest;
import com.btsheng.erp.business.crm.inventoryalert.dto.SafetyStockRequest;
import com.btsheng.erp.business.crm.inventoryalert.entity.CrmInventoryAlert;
import com.btsheng.erp.business.crm.inventoryalert.entity.CrmInventorySafety;
import com.btsheng.erp.business.crm.inventoryalert.mapper.CrmInventoryAlertMapper;
import com.btsheng.erp.business.crm.inventoryalert.mapper.CrmInventorySafetyMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.14 · 安全库存与预�?Service
 *
 * 4 业务方法：updateSafetyConfig / listAlerts / resolveAlert / checkAlert
 * 预警规则：current_stock < min_qty 触发预警
 * 3 P1 修补：阈值非�?/ 4 级别 INFO/WARN/ERROR/CRITICAL / 解决后归�? * 3 P2 修补：定时任务每日检�?/ 邮件 163 邮箱 / 短信 webhook
 */
@Service
public class InventoryAlertService {

    public static final String ALERT_LEVEL_INFO = "INFO";
    public static final String ALERT_LEVEL_WARN = "WARN";
    public static final String ALERT_LEVEL_ERROR = "ERROR";
    public static final String ALERT_LEVEL_CRITICAL = "CRITICAL";

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    /** ERROR 阈�?= min_qty * 0.5 */
    public static final double ERROR_RATIO = 0.5;
    /** WARN 阈�?= min_qty * 0.8 */
    public static final double WARN_RATIO = 0.8;

    private final CrmInventorySafetyMapper safetyMapper;
    private final CrmInventoryAlertMapper alertMapper;

    @Autowired
    public InventoryAlertService(CrmInventorySafetyMapper safetyMapper,
                                  CrmInventoryAlertMapper alertMapper) {
        this.safetyMapper = safetyMapper;
        this.alertMapper = alertMapper;
    }

    /**
     * AC-4.4.1：更新安全库存配�?     */
    @Transactional
    @AuditLog(module = "inventory", action = "inventory.update_safety")
    public Result<CrmInventorySafety> updateSafetyConfig(SafetyStockRequest req, Long operatorUserId) {
        // P1 修补 1：安全库存阈值非�?
            if (req.getMaterialCode() == null || req.getMaterialCode().isEmpty()) {
            return Result.fail(40001, "MATERIAL_CODE_REQUIRED");
        }
        if (req.getMinQty() == null || req.getMinQty() < 0) {
            return Result.fail(40001, "MIN_QTY_NON_NEGATIVE");
        }
        if (req.getMaxQty() == null || req.getMaxQty() < 0) {
            return Result.fail(40001, "MAX_QTY_NON_NEGATIVE");
        }
        if (req.getReorderQty() == null || req.getReorderQty() < 0) {
            return Result.fail(40001, "REORDER_QTY_NON_NEGATIVE");
        }
        if (req.getMinQty() > req.getMaxQty()) {
            return Result.fail(40001, "MIN_GREATER_THAN_MAX");
        }

        CrmInventorySafety existing = safetyMapper.selectByMaterialCode(req.getMaterialCode());
        CrmInventorySafety entity = existing != null ? existing : new CrmInventorySafety();
        entity.setMaterialCode(req.getMaterialCode());
        entity.setMaterialName(req.getMaterialName());
        entity.setMinQty(req.getMinQty());
        entity.setMaxQty(req.getMaxQty());
        entity.setReorderQty(req.getReorderQty());
        entity.setUnit(req.getUnit() != null ? req.getUnit() : "件");
        entity.setEnabled(1);
        entity.setOwnerUserId(operatorUserId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            safetyMapper.insert(entity);
        } else {
            entity.setUpdatedAt(now);
            safetyMapper.updateById(entity);
        }
        return Result.ok(entity);
    }

    /**
     * 获取安全库存配置
     */
    public Result<CrmInventorySafety> getSafetyConfig(String materialCode) {
        CrmInventorySafety entity = safetyMapper.selectByMaterialCode(materialCode);
        if (entity == null) {
            return Result.fail(40404, "SAFETY_CONFIG_NOT_FOUND");
        }
        return Result.ok(entity);
    }

    /**
     * 列出所有安全库存配�?     */
    public Result<List<CrmInventorySafety>> listSafetyConfigs() {
        return Result.ok(safetyMapper.selectAllEnabled());
    }

    /**
     * 列出预警
     */
    public Result<Map<String, Object>> listAlerts(InventoryAlertQueryRequest query) {
        int limit = query.getSize() > 0 ? query.getSize() : 20;
        int offset = Math.max(query.getPage(), 0) * limit;
        List<Map<String, Object>> list = alertMapper.selectAlerts(
            query.getStatus(), query.getLevel(), limit, offset);
        Map<String, Object> page = new HashMap<>();
        page.put("list", list);
        page.put("page", query.getPage());
        page.put("size", limit);
        return Result.ok(page);
    }

    /**
     * 解决预警
     */
    @Transactional
    @AuditLog(module = "inventory", action = "inventory.resolve_alert")
    public Result<CrmInventoryAlert> resolveAlert(Long alertId, AlertResolveRequest req, Long operatorUserId) {
        CrmInventoryAlert alert = alertMapper.selectById(alertId);
        if (alert == null) {
            return Result.fail(40404, "ALERT_NOT_FOUND");
        }
        if (!STATUS_OPEN.equals(alert.getStatus())) {
            return Result.fail(40903, "ALERT_ALREADY_RESOLVED");
        }
        alert.setStatus(STATUS_RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(operatorUserId);
        alert.setResolutionNote(req.getResolutionNote());
        alertMapper.updateById(alert);
        return Result.ok(alert);
    }

    /**
     * 归档预警（P1 修补 3�?     */
    @Transactional
    public Result<CrmInventoryAlert> archiveAlert(Long alertId, Long operatorUserId) {
        CrmInventoryAlert alert = alertMapper.selectById(alertId);
        if (alert == null) {
            return Result.fail(40404, "ALERT_NOT_FOUND");
        }
        if (!STATUS_RESOLVED.equals(alert.getStatus())) {
            return Result.fail(40903, "ALERT_NOT_RESOLVED");
        }
        alert.setStatus(STATUS_ARCHIVED);
        alertMapper.updateById(alert);
        return Result.ok(alert);
    }

    /**
     * AC-4.4.2：检查并触发预警
     * current_qty < min_qty 触发
     * 4 级别：INFO / WARN / ERROR / CRITICAL
     */
    @Transactional
    @AuditLog(module = "inventory", action = "inventory.check_alert")
    public Result<CrmInventoryAlert> checkAlert(String materialCode, int currentQty) {
        CrmInventorySafety safety = safetyMapper.selectByMaterialCode(materialCode);
        if (safety == null) {
            return Result.fail(40404, "SAFETY_CONFIG_NOT_FOUND");
        }
        int min = safety.getMinQty();
        if (currentQty >= min) {
            return Result.ok(null);  // 库存正常
        }

        // 4 级别判断
            String level;
        String message;
        if (currentQty == 0) {
            level = ALERT_LEVEL_CRITICAL;
            message = materialCode + " 库存�?0 严重缺货";
        } else if (currentQty < min * ERROR_RATIO) {
            level = ALERT_LEVEL_ERROR;
            message = materialCode + " 库存 " + currentQty + " 严重低于安全库存 " + min;
        } else if (currentQty < min * WARN_RATIO) {
            level = ALERT_LEVEL_WARN;
            message = materialCode + " 库存 " + currentQty + " 接近安全库存 " + min;
        } else {
            level = ALERT_LEVEL_INFO;
            message = materialCode + " 库存 " + currentQty + " 低于安全库存 " + min;
        }

        // 查重：同一物料 OPEN 预警
            CrmInventoryAlert existing = alertMapper.selectOpenByMaterial(materialCode);
        if (existing != null) {
            return Result.ok(existing);
        }

        CrmInventoryAlert alert = new CrmInventoryAlert();
        alert.setMaterialCode(materialCode);
        alert.setAlertLevel(level);
        alert.setCurrentQty(currentQty);
        alert.setMinQty(min);
        alert.setMessage(message);
        alert.setStatus(STATUS_OPEN);
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setNotified(0);
        alertMapper.insert(alert);
        return Result.ok(alert);
    }

    /**
     * 预警统计
     */
    public Result<List<Map<String, Object>>> alertStats() {
        return Result.ok(alertMapper.aggregateByLevel());
    }
}
