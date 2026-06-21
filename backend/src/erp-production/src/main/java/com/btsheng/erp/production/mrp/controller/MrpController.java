package com.btsheng.erp.production.mrp.controller;

import com.btsheng.erp.production.mrp.dto.MrpRunRequest;
import com.btsheng.erp.production.mrp.dto.MrpRunResponse;
import com.btsheng.erp.production.mrp.entity.CrmMrpResult;
import com.btsheng.erp.production.mrp.entity.CrmMrpRun;
import com.btsheng.erp.production.mrp.entity.CrmMrpShortage;
import com.btsheng.erp.production.mrp.service.MrpService;
import com.btsheng.erp.production.mrp.service.MrpTriggerService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.17 · MRP 物料需求分析 Controller
 *
 * 3 端点：
 * - POST /mrp/run                 MRP 运行（AC-5.3.1 · AC-5.3.2）
 * - GET  /mrp/results             MRP 结果查询（AC-5.3.3）
 * - GET  /mrp/shortages           缺料清单
 */
@RestController
@RequestMapping("/mrp")
@Tag(name = "E5-MRP", description = "MRP 物料需求分析（Story 1.17）")
public class MrpController {

    private final MrpService service;
    private final MrpTriggerService mrpTriggerService;

    @Autowired
    public MrpController(MrpService service, MrpTriggerService mrpTriggerService) {
        this.service = service;
        this.mrpTriggerService = mrpTriggerService;
    }

    @PostMapping("/run")
    @Operation(summary = "MRP 运行（手动兜底 · AC-5.3.1）")
    public Result<MrpRunResponse> runMrp(
            @RequestBody MrpRunRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        req.setTriggerType(MrpTriggerService.TRIGGER_MANUAL);
        req.setTriggerSource("MRP_CENTER");
        return mrpTriggerService.triggerRun(MrpTriggerService.TRIGGER_MANUAL, "MRP_CENTER", req, userId);
    }

    @GetMapping("/results")
    @Operation(summary = "MRP 结果查询（AC-5.3.3 · 按 run_id）")
    public Result<List<CrmMrpResult>> getMrpResult(@RequestParam Long runId) {
        return service.getMrpResult(runId);
    }

    @GetMapping("/shortages")
    @Operation(summary = "MRP 缺料清单")
    public Result<List<CrmMrpShortage>> listShortages(@RequestParam Long runId) {
        return service.listShortages(runId);
    }

    @GetMapping("/shortages/by-material/{materialCode}")
    @Operation(summary = "按物料编码查缺料")
    public Result<List<CrmMrpShortage>> listShortagesByMaterial(@PathVariable String materialCode) {
        return service.listShortagesByMaterial(materialCode);
    }

    @GetMapping("/runs")
    @Operation(summary = "MRP 运算历史")
    public Result<Map<String, Object>> listRuns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listRuns(status, page, size);
    }

    @PostMapping("/export-to-purchase")
    @Operation(summary = "MRP 缺料导出到采购（P2 修补 2 · 1.32 闭环）")
    public Result<Map<String, Object>> exportToPurchase(
            @RequestParam Long runId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.exportMrpToPurchase(runId, userId);
    }
}
