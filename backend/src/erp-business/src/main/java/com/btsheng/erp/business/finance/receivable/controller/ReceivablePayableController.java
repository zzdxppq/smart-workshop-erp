package com.btsheng.erp.business.finance.receivable.controller;

import com.btsheng.erp.business.finance.receivable.dto.CreatePayableRequest;
import com.btsheng.erp.business.finance.receivable.dto.CreateReceivableRequest;
import com.btsheng.erp.business.finance.receivable.dto.RecordPaymentRequest;
import com.btsheng.erp.business.finance.receivable.entity.CrmPayable;
import com.btsheng.erp.business.finance.receivable.entity.CrmReceivable;
import com.btsheng.erp.business.finance.receivable.service.ReceivablePayableService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * V1.3.7 · Story 1.36 · 财务·应收应付 Controller (FR-9-1)
 *
 * <p>5 端点：
 * <ul>
 *   <li>POST /receivable           创建应收</li>
 *   <li>POST /payable              创建应付</li>
 *   <li>POST /payment              收/付款</li>
 *   <li>GET  /receivable/aging     账龄</li>
 *   <li>POST /receivable/pending   待办</li>
 * </ul>
 */
@RestController
@RequestMapping("")
@Tag(name = "E9-Receivable-Payable", description = "财务·应收应付（Story 1.36 FR-9-1）")
public class ReceivablePayableController {

    private final ReceivablePayableService service;

    @Autowired
    public ReceivablePayableController(ReceivablePayableService service) {
        this.service = service;
    }

    @PostMapping("/receivable")
    @Operation(summary = "创建应收（AC-9.1.1 · 订单 SETTLED 触发）")
    public Result<CrmReceivable> createReceivable(
            @RequestBody CreateReceivableRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "701") Long userId) {
        return service.createReceivable(req, userId);
    }

    @PostMapping("/payable")
    @Operation(summary = "创建应付（AC-9.1.1 · PO 触发）")
    public Result<CrmPayable> createPayable(
            @RequestBody CreatePayableRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "702") Long userId) {
        return service.createPayable(req, userId);
    }

    @PostMapping("/payment")
    @Operation(summary = "收/付款（AC-9.1.2 · 收 ≤ 未收 / 付 ≤ 未付）")
    public Result<Map<String, Object>> recordPayment(
            @RequestBody RecordPaymentRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "701") Long userId) {
        return service.recordPayment(req, userId);
    }

    @GetMapping("/receivable/aging")
    @Operation(summary = "账龄分析（4 段 30/60/90/90+）")
    public Result<Map<String, Object>> aging() {
        return service.getAging();
    }

    @PostMapping("/receivable/pending")
    @Operation(summary = "待办（未结清应收 + 应付）")
    public Result<Map<String, Object>> pending() {
        return service.listPending();
    }
}
