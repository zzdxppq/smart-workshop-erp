package com.btsheng.erp.business.finance.payment.controller;

import com.btsheng.erp.business.finance.payment.dto.CreatePlanRequest;
import com.btsheng.erp.business.finance.payment.dto.MarkPaidRequest;
import com.btsheng.erp.business.finance.payment.entity.CrmPaymentPlan;
import com.btsheng.erp.business.finance.payment.service.PaymentCollectionService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.38 · 财务·回款控制 Controller (FR-9-3)
 *
 * <p>4 端点：
 * <ul>
 *   <li>POST /payment-plan          创建回款计划（AC-9.3.1）</li>
 *   <li>POST /payment-plan/list     待办（PENDING/ALERT）</li>
 *   <li>POST /payment-plan/{id}/paid 标记回款</li>
 *   <li>GET  /payment-plan/overdue  逾期（ALERT_CRITICAL）</li>
 * </ul>
 */
@RestController
@RequestMapping("/payment-plan")
@Tag(name = "E9-Payment-Collection", description = "财务·回款控制（Story 1.38 FR-9-3）")
public class PaymentCollectionController {

    private final PaymentCollectionService service;

    @Autowired
    public PaymentCollectionController(PaymentCollectionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建回款计划（AC-9.3.1）")
    public Result<CrmPaymentPlan> create(
            @RequestBody CreatePlanRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "701") Long userId) {
        return service.createPlan(req, userId);
    }

    @PostMapping("/list")
    @Operation(summary = "待办回款（PENDING + 提前 ALERT）")
    public Result<List<CrmPaymentPlan>> list() {
        return service.listPendingPlans();
    }

    @PostMapping("/{id}/paid")
    @Operation(summary = "标记回款")
    public Result<CrmPaymentPlan> paid(
            @PathVariable Long id,
            @RequestBody MarkPaidRequest req) {
        return service.markPaid(id, req, 701L);
    }

    @GetMapping("/overdue")
    @Operation(summary = "逾期（ALERT_CRITICAL）")
    public Result<Map<String, Object>> overdue() {
        return service.getOverduePlans();
    }
}
