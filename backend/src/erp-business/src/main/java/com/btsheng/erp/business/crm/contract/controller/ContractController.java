package com.btsheng.erp.business.crm.contract.controller;

import com.btsheng.erp.business.crm.contract.service.ContractService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.PreAuthorizeRoles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contracts")
@Tag(name = "E2-Contracts", description = "合同回款（Story 1.7）")
public class ContractController {

    private final ContractService service;

    @Autowired
    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "合同列表")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.list(keyword, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "合同回款详情（关联销售订单）")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        return service.getContract(id);
    }

    @GetMapping("/{id}/payment-plan")
    @Operation(summary = "回款计划")
    public Result<List<Map<String, Object>>> paymentPlan(@PathVariable Long id) {
        return service.paymentPlan(id);
    }

    @GetMapping("/{id}/payment-reg")
    @Operation(summary = "收款登记")
    public Result<List<Map<String, Object>>> paymentReg(@PathVariable Long id) {
        return service.paymentReg(id);
    }

    @PostMapping("/{id}/payment-plan")
    @PreAuthorize(PreAuthorizeRoles.SALES)
    @Operation(summary = "新增回款计划（销售登记）")
    public Result<Map<String, Object>> createPaymentPlan(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return service.savePaymentPlan(id, null, body);
    }

    @PutMapping("/{id}/payment-plan/{planId}")
    @PreAuthorize(PreAuthorizeRoles.SALES)
    @Operation(summary = "更新回款计划（销售登记）")
    public Result<Map<String, Object>> updatePaymentPlan(@PathVariable Long id, @PathVariable Long planId,
                                                         @RequestBody Map<String, Object> body) {
        return service.savePaymentPlan(id, planId, body);
    }

    @DeleteMapping("/{id}/payment-plan/{planId}")
    @PreAuthorize(PreAuthorizeRoles.SALES)
    @Operation(summary = "删除回款计划（销售登记）")
    public Result<Void> deletePaymentPlan(@PathVariable Long id, @PathVariable Long planId) {
        return service.deletePaymentPlan(id, planId);
    }

    @PostMapping("/{id}/payment-reg")
    @PreAuthorize(PreAuthorizeRoles.FINANCE)
    @Operation(summary = "登记收款（财务登记）")
    public Result<Map<String, Object>> createPaymentReg(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return service.savePaymentReg(id, null, body);
    }

    @PutMapping("/{id}/payment-reg/{receiptId}")
    @PreAuthorize(PreAuthorizeRoles.FINANCE)
    @Operation(summary = "更新收款（财务登记）")
    public Result<Map<String, Object>> updatePaymentReg(@PathVariable Long id, @PathVariable Long receiptId,
                                                        @RequestBody Map<String, Object> body) {
        return service.savePaymentReg(id, receiptId, body);
    }

    @DeleteMapping("/{id}/payment-reg/{receiptId}")
    @PreAuthorize(PreAuthorizeRoles.FINANCE)
    @Operation(summary = "删除收款（财务登记）")
    public Result<Void> deletePaymentReg(@PathVariable Long id, @PathVariable Long receiptId) {
        return service.deletePaymentReg(id, receiptId);
    }

    @GetMapping("/{id}/profit")
    @Operation(summary = "订单利润")
    public Result<List<Map<String, Object>>> profit(@PathVariable Long id) {
        return service.profit(id);
    }
}
