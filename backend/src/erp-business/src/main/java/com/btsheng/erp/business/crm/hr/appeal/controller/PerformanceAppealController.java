package com.btsheng.erp.business.crm.hr.appeal.controller;

import com.btsheng.erp.business.crm.hr.appeal.dto.PerformanceAppealRequest;
import com.btsheng.erp.business.crm.hr.appeal.dto.PerformanceAppealResolveRequest;
import com.btsheng.erp.business.crm.hr.appeal.entity.CrmHrPerformanceAppeal;
import com.btsheng.erp.business.crm.hr.appeal.service.PerformanceAppealService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "E10-HR-Appeal", description = "人事·绩效申诉")
@RestController
@RequestMapping("/hr/performance-appeals")
public class PerformanceAppealController {

    private final PerformanceAppealService appealService;

    @Autowired
    public PerformanceAppealController(PerformanceAppealService appealService) {
        this.appealService = appealService;
    }

    @Operation(summary = "员工提交绩效申诉")
    @PostMapping
    public Result<CrmHrPerformanceAppeal> submit(@RequestBody PerformanceAppealRequest req,
                                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return appealService.submit(req, userId == null ? 1L : userId);
    }

    @Operation(summary = "我的绩效申诉")
    @GetMapping("/my")
    public Result<Map<String, Object>> my(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return appealService.myAppeals(userId == null ? 1L : userId, page, size);
    }

    @Operation(summary = "申诉列表（HR）")
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return appealService.list(employeeId, status, page, size);
    }

    @Operation(summary = "处理申诉")
    @PostMapping("/{id}/resolve")
    public Result<CrmHrPerformanceAppeal> resolve(@PathVariable Long id,
                                                   @RequestBody PerformanceAppealResolveRequest req,
                                                   @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return appealService.resolve(id, req, userId == null ? 1L : userId);
    }
}
