package com.btsheng.erp.business.crm.hr.payroll.controller;

import com.btsheng.erp.business.crm.hr.payroll.dto.PayrollCalculateRequest;
import com.btsheng.erp.business.crm.hr.payroll.entity.CrmHrPayroll;
import com.btsheng.erp.business.crm.hr.payroll.service.PayrollService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "E10-HR-Payroll", description = "人事·薪酬核算")
@RestController
@RequestMapping("/hr/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    @Autowired
    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @Operation(summary = "薪酬核算")
    @PostMapping("/calculate")
    public Result<CrmHrPayroll> calculate(@RequestBody PayrollCalculateRequest req,
                                         @RequestParam(required = false) Long operatorUserId) {
        return payrollService.calculatePayroll(req, operatorUserId == null ? 1L : operatorUserId);
    }

    @Operation(summary = "批量薪酬核算（全员·账套+计件+绩效）")
    @PostMapping("/calculate-batch")
    public Result<Map<String, Object>> calculateBatch(
            @RequestParam String period,
            @RequestParam(required = false) Long operatorUserId) {
        return payrollService.calculateBatchPayroll(period, operatorUserId == null ? 1L : operatorUserId);
    }

    @Operation(summary = "工资条 HTML（可打印/PDF）")
    @GetMapping("/{id}/slip")
    public Result<String> slip(@PathVariable Long id) {
        return payrollService.payrollSlipHtml(id);
    }

    @Operation(summary = "薪酬单列表（Web 路径）")
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return payrollService.listPayrolls(period, pageNum, pageSize);
    }

    @Operation(summary = "薪酬单详情")
    @GetMapping("/{id}")
    public Result<CrmHrPayroll> get(@PathVariable Long id) {
        return payrollService.getPayroll(id);
    }

    @Operation(summary = "我的薪酬历史")
    @GetMapping("/my")
    public Result<Map<String, Object>> my(@RequestParam(required = false) Long employeeId,
                                          @RequestParam(required = false) Integer year,
                                          @RequestParam(required = false) Integer month,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (employeeId != null) {
            return payrollService.getMyPayrollHistory(employeeId, year, month, status, page, size);
        }
        return payrollService.getMyPayrollForUser(userId == null ? 1L : userId, year, month, status, page, size);
    }

    @Operation(summary = "工资条 PDF")
    @GetMapping("/{id}/slip.pdf")
    public org.springframework.http.ResponseEntity<byte[]> slipPdf(@PathVariable Long id) {
        Result<byte[]> r = payrollService.payrollSlipPdf(id);
        if (!r.isSuccess() || r.getData() == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payslip-" + id + ".pdf")
                .body(r.getData());
    }

    @Operation(summary = "审批薪酬单")
    @PostMapping("/{id}/approve")
    public Result<CrmHrPayroll> approve(@PathVariable Long id,
                                        @RequestParam(required = false) Long operatorUserId) {
        return payrollService.approvePayroll(id, operatorUserId == null ? 1L : operatorUserId);
    }
}
