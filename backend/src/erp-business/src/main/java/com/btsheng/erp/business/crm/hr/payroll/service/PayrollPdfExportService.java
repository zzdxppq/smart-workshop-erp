package com.btsheng.erp.business.crm.hr.payroll.service;

import com.btsheng.erp.business.crm.hr.payroll.entity.CrmHrPayroll;
import com.btsheng.erp.core.model.Result;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Service
public class PayrollPdfExportService {

    public Result<byte[]> exportPdf(CrmHrPayroll p) {
        if (p == null) return Result.fail(40401, "PAYROLL_NOT_FOUND");
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        sb.append("1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n");
        sb.append("2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n");
        sb.append("3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R >>endobj\n");
        String text = buildSlipText(p);
        String stream = "BT /F1 12 Tf 50 780 Td (" + escapePdf(text) + ") Tj ET";
        sb.append("4 0 obj<< /Length ").append(stream.length()).append(" >>stream\n");
        sb.append(stream).append("\nendstream endobj\n");
        sb.append("xref\n0 5\n0000000000 65535 f \n");
        sb.append("trailer<< /Root 1 0 R >>\nstartxref\n0\n%%EOF\n");
        sb.append("\n%% PAYSLIP ").append(p.getPayrollNo()).append("\n");
        sb.append(text);
        return Result.ok(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String buildSlipText(CrmHrPayroll p) {
        return String.format(
                "PAYSLIP %s | %s (%s) | %d-%02d | base=%s position=%s piece=%s perf=%s overtime=%s full=%s deduct=%s social=%s tax=%s NET=%s",
                p.getPayrollNo(), p.getEmployeeName(), p.getEmployeeNo(),
                p.getPeriodYear(), p.getPeriodMonth(),
                nz(p.getBaseSalary()), nz(p.getPositionSalary()), nz(p.getPiecePay()), nz(p.getPerformanceBonus()),
                nz(p.getOvertimePay()), nz(p.getFullAttendanceBonus()), nz(p.getDeduction()),
                nz(p.getSocialInsurance()), nz(p.getTax()), p.getNetSalary());
    }

    private String escapePdf(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
