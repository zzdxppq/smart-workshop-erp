package com.btsheng.erp.business.crm.hr.performance.controller;

import com.btsheng.erp.business.crm.hr.performance.dto.PerformanceRequest;
import com.btsheng.erp.business.crm.hr.performance.dto.RecruitmentRequest;
import com.btsheng.erp.business.crm.hr.performance.entity.CrmHrPerformance;
import com.btsheng.erp.business.crm.hr.performance.entity.CrmHrRecruitment;
import com.btsheng.erp.business.crm.hr.performance.service.PerformanceRecruitmentService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "E10-HR-Performance", description = "人事·绩效与招聘")
@RestController
@RequestMapping("/hr")
public class PerformanceRecruitmentController {

    private final PerformanceRecruitmentService service;

    @Autowired
    public PerformanceRecruitmentController(PerformanceRecruitmentService service) {
        this.service = service;
    }

    @Operation(summary = "绩效录入")
    @PostMapping("/performance")
    public Result<CrmHrPerformance> addPerformance(@RequestBody PerformanceRequest req,
                                                    @RequestParam(required = false) Long operatorUserId) {
        return service.addPerformance(req, operatorUserId == null ? 1L : operatorUserId);
    }

    @Operation(summary = "绩效查询")
    @GetMapping("/performance")
    public Result<Map<String, Object>> listPerformance(@RequestParam(required = false) Long employeeId,
                                                       @RequestParam(required = false) Integer year,
                                                       @RequestParam(required = false) Integer month,
                                                       @RequestParam(required = false) String period,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        if (period != null && period.length() >= 7 && year == null) {
            year = Integer.parseInt(period.substring(0, 4));
            month = Integer.parseInt(period.substring(5, 7));
        }
        return service.getPerformance(employeeId, year, month, page, size);
    }

    @Operation(summary = "批量自动核算绩效（报工+考勤）")
    @PostMapping("/performance/calculate")
    public Result<Map<String, Object>> calculatePerformance(
            @RequestParam String period,
            @RequestParam(required = false) Long operatorUserId) {
        return service.calculateBatchPerformance(period, operatorUserId == null ? 1L : operatorUserId);
    }

    @Operation(summary = "我的绩效记录")
    @GetMapping("/performance/my")
    public Result<Map<String, Object>> myPerformance(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.myPerformance(userId == null ? 1L : userId, year, month, page, size);
    }

    @Operation(summary = "招聘录入")
    @PostMapping("/recruitment")
    public Result<CrmHrRecruitment> addRecruitment(@RequestBody RecruitmentRequest req,
                                                    @RequestParam(required = false) Long operatorUserId) {
        return service.addRecruitment(req, operatorUserId == null ? 1L : operatorUserId);
    }

    @Operation(summary = "招聘列表（Web 路径）")
    @GetMapping("/recruitment")
    public Result<Map<String, Object>> listRecruitment(
            @RequestParam(required = false) String finalStatus,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.listRecruitments(finalStatus, pageNum, pageSize);
    }

    @Operation(summary = "招聘详情")
    @GetMapping("/recruitment/{id}")
    public Result<CrmHrRecruitment> getRecruitment(@PathVariable Long id) {
        return service.getRecruitment(id);
    }
}
