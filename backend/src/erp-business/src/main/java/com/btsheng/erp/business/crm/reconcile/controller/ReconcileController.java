package com.btsheng.erp.business.crm.reconcile.controller;

import com.btsheng.erp.business.crm.reconcile.dto.ReconcileCreateRequest;
import com.btsheng.erp.business.crm.reconcile.dto.ReconcileItemRequest;
import com.btsheng.erp.business.crm.reconcile.dto.ReconcileVendorConfirmRequest;
import com.btsheng.erp.business.crm.reconcile.entity.CrmReconcile;
import com.btsheng.erp.business.crm.reconcile.entity.CrmReconcileItem;
import com.btsheng.erp.business.crm.reconcile.entity.CrmReconcileSignature;
import com.btsheng.erp.business.crm.reconcile.service.ReconcileService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.21 · 月度对账 Controller (FR-6-1)
 *
 * 5 端点：
 * - POST   /reconciles                          创建月度对账单（AC-6.1.1）
 * - GET    /reconciles/{id}                     对账单详情
 * - POST   /reconciles/{id}/items               追加对账明细
 * - POST   /reconciles/{id}/upload-signature    厂商签字扫描件上传（AC-6.1.3 · AES-256-GCM）
 * - POST   /reconciles/{id}/vendor-confirm      厂商对账确认（AC-6.1.2 · 40905）
 * - POST   /reconciles/{id}/both-confirm        双方对账确认
 * - POST   /reconciles/{id}/finance-confirm     财务对账确认（AC-6.1.4）
 * - GET    /reconciles                          对账列表
 *
 * V1.3.7 AD-2 红线：不含"线下"动作
 */
@RestController
@RequestMapping("/reconciles")
@Tag(name = "E6-Reconcile", description = "月度对账不含\"线下\"动作（V1.3.7 AD-2 红线 · Story 1.21）")
public class ReconcileController {

    private final ReconcileService service;

    @Autowired
    public ReconcileController(ReconcileService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建月度对账单（AC-6.1.1 · RC{yyyyMM}{seq:4}）")
    public Result<CrmReconcile> createReconcile(
            @RequestBody ReconcileCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createReconcile(req, userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "对账单详情（主单 + 明细 + 签字）")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        return service.getReconcileDetail(id);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "追加对账明细（AC-6.1.1）")
    public Result<CrmReconcileItem> addItem(
            @PathVariable Long id,
            @RequestBody ReconcileItemRequest req) {
        return service.addItem(id, req);
    }

    @PostMapping("/{id}/upload-signature")
    @Operation(summary = "上传厂商签字扫描件（AC-6.1.3 · AES-256-GCM 加密 · 厂商签字必传 P1 修补）")
    public Result<CrmReconcileSignature> uploadSignature(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("signerName") String signerName,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) throws IOException {
        return service.uploadSignature(id, file.getBytes(), signerName, userId);
    }

    @PostMapping("/{id}/vendor-confirm")
    @Operation(summary = "厂商对账确认（AC-6.1.2 · 40905 金额不一致）")
    public Result<CrmReconcile> vendorConfirm(
            @PathVariable Long id,
            @RequestBody ReconcileVendorConfirmRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.vendorConfirm(id, req, userId);
    }

    @PostMapping("/{id}/both-confirm")
    @Operation(summary = "双方对账确认 → step 3")
    public Result<CrmReconcile> bothConfirm(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.bothConfirm(id, userId);
    }

    @PostMapping("/{id}/finance-confirm")
    @Operation(summary = "财务对账确认（AC-6.1.4 · → step 4 → CLOSED）")
    public Result<CrmReconcile> financeConfirm(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.financeConfirm(id, userId);
    }

    @GetMapping
    @Operation(summary = "对账列表（按 vendor/period/status 过滤）")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Integer periodYear,
            @RequestParam(required = false) Integer periodMonth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listReconciles(vendorId, periodYear, periodMonth, status, page, size);
    }
}
