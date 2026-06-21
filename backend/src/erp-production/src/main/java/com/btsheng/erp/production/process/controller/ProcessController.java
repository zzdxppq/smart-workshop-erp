package com.btsheng.erp.production.process.controller;

import com.btsheng.erp.production.process.dto.*;
import com.btsheng.erp.production.process.entity.CrmProcess;
import com.btsheng.erp.production.process.entity.CrmProcessRoute;
import com.btsheng.erp.production.process.entity.CrmProcessStep;
import com.btsheng.erp.production.process.service.ProcessService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * V1.3.7 · Story 1.10 · 工艺库与工序 控制器
 *
 * 4 端点：
 * 1) POST /processes                 创建工艺（AC-3.4.1）
 * 2) POST /processes/{id}/steps      新增工序（AC-3.4.2）
 * 3) GET  /processes/{id}/route      工艺路线（AC-3.4.3）
 * 4) POST /processes/{id}/bind-to-drawing  绑定图纸（AC-3.4.4）
 * 5) GET  /processes                 列表
 */
@Tag(name = "E3-Process", description = "工艺库与工序（Story 1.10 · AC-3.4 · 5 段成本聚合）")
@RestController
@RequestMapping("/processes")
public class ProcessController {

    private final ProcessService processService;

    @Autowired
    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }

    @Operation(summary = "创建工艺（AC-3.4.1 · 5 段成本自动聚合）")
    @PostMapping
    public Result<CrmProcess> createProcess(
            @RequestBody ProcessCreateRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        return processService.createProcess(req, operatorUserId == null ? 0L : operatorUserId);
    }

    @Operation(summary = "新增工序（AC-3.4.2 · step_no 自动 max+1）")
    @PostMapping("/{id}/steps")
    public Result<CrmProcessStep> addStep(
            @PathVariable("id") Long processId,
            @RequestBody AddStepRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        return processService.addStep(processId, req, operatorUserId == null ? 0L : operatorUserId);
    }

    @Operation(summary = "查询工艺路线（AC-3.4.3）")
    @GetMapping("/{id}/route")
    public Result<Map<String, Object>> getRoute(
            @PathVariable("id") Long processId,
            @Parameter(description = "版本号过滤") @RequestParam(value = "version", required = false) String version) {
        return processService.getRoute(processId, version);
    }

    @Operation(summary = "工艺路线绑定图纸（AC-3.4.4 · P2 修补：工艺变更历史）")
    @PostMapping("/{id}/bind-to-drawing")
    public Result<CrmProcessRoute> bindToDrawing(
            @PathVariable("id") Long processId,
            @RequestBody BindRouteRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        return processService.bindToDrawing(processId, req, operatorUserId == null ? 0L : operatorUserId);
    }

    @Operation(summary = "工艺列表查询")
    @GetMapping
    public Result<Map<String, Object>> listProcesses(ProcessQueryRequest query) {
        return processService.listProcesses(query);
    }

    @Operation(summary = "工艺详情")
    @GetMapping("/{id}")
    public Result<CrmProcess> getProcess(@PathVariable("id") Long processId) {
        return processService.getProcess(processId);
    }

    @Operation(summary = "更新工艺")
    @PutMapping("/{id}")
    public Result<CrmProcess> updateProcess(
            @PathVariable("id") Long processId,
            @RequestBody ProcessUpdateRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        return processService.updateProcess(processId, req, operatorUserId == null ? 0L : operatorUserId);
    }
}
