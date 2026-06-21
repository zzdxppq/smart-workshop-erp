package com.btsheng.erp.business.crm.incomingalert.controller;

import com.btsheng.erp.business.crm.incomingalert.dto.CreateAlertRequest;
import com.btsheng.erp.business.crm.incomingalert.dto.MarkArrivedRequest;
import com.btsheng.erp.business.crm.incomingalert.entity.CrmIncomingAlert;
import com.btsheng.erp.business.crm.incomingalert.service.IncomingAlertService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.34 · 采购·到货提醒 Controller (FR-8-3)
 *
 * <p>3 端点：
 * <ul>
 *   <li>POST /incoming-alert              创建提醒（AC-8.3.1 · PO hook）</li>
 *   <li>POST /incoming-alert/list         在途 + 提前 + 逾期（2 视图）</li>
 *   <li>POST /incoming-alert/{id}/arrived 标记到货</li>
 *   <li>GET  /incoming-alert/overdue      逾期（ALERT_CRITICAL）</li>
 * </ul>
 */
@RestController
@RequestMapping("/incoming-alert")
@Tag(name = "E8-Incoming-Alert", description = "采购·到货提醒（Story 1.34 FR-8-3）")
public class IncomingAlertController {

    private final IncomingAlertService service;

    @Autowired
    public IncomingAlertController(IncomingAlertService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建到货提醒（AC-8.3.1 · PO hook）")
    public Result<CrmIncomingAlert> create(
            @RequestBody CreateAlertRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createAlert(req, userId);
    }

    @PostMapping("/list")
    @Operation(summary = "在途 + 提前 + 逾期（2 视图 · 红黄灯 3 档）")
    public Result<List<CrmIncomingAlert>> list(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.listPendingAlerts();
    }

    @PostMapping("/{id}/arrived")
    @Operation(summary = "标记到货")
    public Result<Map<String, Object>> markArrived(
            @PathVariable Long id,
            @RequestBody MarkArrivedRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.markArrived(id, req, userId);
    }

    @GetMapping("/overdue")
    @Operation(summary = "逾期提醒（ALERT_CRITICAL）")
    public Result<List<CrmIncomingAlert>> overdue(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.getOverdueAlerts();
    }
}
