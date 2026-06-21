package com.btsheng.erp.production.scan.controller;

import com.btsheng.erp.production.scan.dto.ScanPendingResponse;
import com.btsheng.erp.production.scan.dto.ScanReportRequest;
import com.btsheng.erp.production.scan.dto.ScanStartRequest;
import com.btsheng.erp.production.scan.dto.ScanStationRequest;
import com.btsheng.erp.production.scan.entity.CrmProductionReport;
import com.btsheng.erp.production.scan.entity.CrmProductionScan;
import com.btsheng.erp.production.scan.entity.CrmProductionStation;
import com.btsheng.erp.production.scan.service.ProductionScanService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.16 · APP 扫码开工/报工/过站 Controller
 *
 * 5 端点：
 * - POST /app/production/scan/start          扫码开工（AC-5.2.1）
 * - POST /app/production/scan/report         扫码报工（AC-5.2.2）
 * - POST /app/production/scan/station        扫码过站（AC-5.2.3）
 * - GET  /app/production/scan/pending        待办列表（AC-5.2.4）
 * - GET  /app/production/scan/history        扫码历史
 */
@RestController
@RequestMapping("/app/production/scan")
@Tag(name = "E5-Production-Scan", description = "APP 扫码开工/报工/过站（Story 1.16）")
public class ProductionScanController {

    private final ProductionScanService service;

    @Autowired
    public ProductionScanController(ProductionScanService service) {
        this.service = service;
    }

    @PostMapping("/start")
    @Operation(summary = "扫码开工（AC-5.2.1 · GD- 工单码）")
    public Result<CrmProductionScan> scanStart(
            @RequestBody ScanStartRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.scanStart(req, userId);
    }

    @PostMapping("/report")
    @Operation(summary = "扫码报工（AC-5.2.2 · P1 修补 报工数量 ≤ 工序数量）")
    public Result<CrmProductionReport> scanReport(
            @RequestBody ScanReportRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.scanReport(req, userId);
    }

    @PostMapping("/station")
    @Operation(summary = "扫码过站（AC-5.2.3 · P1 修补 顺序严格）")
    public Result<CrmProductionStation> scanStation(
            @RequestBody ScanStationRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.scanStationChange(req, userId);
    }

    @GetMapping("/pending")
    @Operation(summary = "待办列表（AC-5.2.4）")
    public Result<ScanPendingResponse> listPending(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.listPending(userId);
    }

    @GetMapping("/history")
    @Operation(summary = "扫码历史")
    public Result<Map<String, Object>> listScans(
            @RequestParam(required = false) String workorderNo,
            @RequestParam(required = false) String scanType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listScans(workorderNo, scanType, page, size);
    }

    @GetMapping("/history/{workorderNo}")
    @Operation(summary = "按工单查扫码历史")
    public Result<List<CrmProductionScan>> getScanHistory(@PathVariable String workorderNo) {
        return service.getScanHistory(workorderNo);
    }

    @GetMapping("/reports/{workorderNo}")
    @Operation(summary = "报工历史")
    public Result<List<CrmProductionReport>> getReportHistory(@PathVariable String workorderNo) {
        return service.getReportHistory(workorderNo);
    }

    @GetMapping("/stations/{workorderNo}")
    @Operation(summary = "过站历史")
    public Result<List<CrmProductionStation>> getStationHistory(@PathVariable String workorderNo) {
        return service.getStationHistory(workorderNo);
    }
}
