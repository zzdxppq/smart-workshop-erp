package com.btsheng.erp.business.crm.gmsummary.controller;

import com.btsheng.erp.business.crm.gmsummary.dto.GmSummaryDTO;
import com.btsheng.erp.business.crm.gmsummary.service.GmSummaryService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * V1.3.8 · Story 4.3 · 总经理汇总报表 Controller
 *
 * <p>1 端点（与 Story 4.3 端点契约一致）：
 * <ul>
 *   <li>GET /reports/gm-summary  AC-4.3.1 + AC-4.3.2</li>
 * </ul>
 *
 * <p>权限：@PreAuthorize("hasAnyRole('GM', 'PROCUREMENT_MANAGER')")
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@RestController
@RequestMapping("/reports")
@Tag(name = "V1.3.8-Story4.3-总经理汇总报表")
public class GmSummaryController {

    private final GmSummaryService service;

    @Autowired
    public GmSummaryController(GmSummaryService service) {
        this.service = service;
    }

    @GetMapping("/gm-summary")
    @PreAuthorize("hasAnyRole('GM', 'PROCUREMENT_MANAGER', 'ADMIN', 'SYS_ADMIN')")
    @Operation(summary = "总经理汇总报表（Story 4.3 AC-4.3.1）")
    public Result<GmSummaryDTO> gmSummary(
            @RequestParam(value = "period", defaultValue = "LAST_30D") String period,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return service.getSummary(period, startDate, endDate);
    }
}