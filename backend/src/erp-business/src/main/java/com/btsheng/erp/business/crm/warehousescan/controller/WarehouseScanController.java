package com.btsheng.erp.business.crm.warehousescan.controller;

import com.btsheng.erp.business.crm.warehousescan.dto.ScanInboundRequest;
import com.btsheng.erp.business.crm.warehousescan.dto.ScanOfflineSyncRequest;
import com.btsheng.erp.business.crm.warehousescan.dto.ScanOutboundRequest;
import com.btsheng.erp.business.crm.warehousescan.dto.ScanQueryRequest;
import com.btsheng.erp.business.crm.warehousescan.dto.ScanResponse;
import com.btsheng.erp.business.crm.warehousescan.entity.CrmWarehouseLocation;
import com.btsheng.erp.business.crm.warehousescan.entity.CrmWarehouseScan;
import com.btsheng.erp.business.crm.warehousescan.service.WarehouseScanService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.12 · APP 扫码出入库 Controller
 *
 * 4 端点（业务）：
 * - POST /app/scan/inbound   扫码入库（AC-4.2.1）
 * - POST /app/scan/outbound  扫码出库（AC-4.2.2）
 * - POST /app/scan/sync      离线同步（AC-4.2.3）
 * - GET  /app/scan/list      扫码历史
 * 4 辅助端点（库位/库存）：
 * - GET  /app/scan/locations       库位列表
 * - GET  /app/scan/locations/recommend 库位推荐
 * - GET  /app/scan/inventory-sync  库存同步（AC-4.2.4）
 * - GET  /app/scan/history/{barcodeNo} 扫码追溯
 */
@RestController
@RequestMapping("/app/scan")
@Tag(name = "E4-Warehouse-Scan", description = "APP 扫码出入库（Story 1.12）")
public class WarehouseScanController {

    private final WarehouseScanService scanService;

    @Autowired
    public WarehouseScanController(WarehouseScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping("/inbound")
    @Operation(summary = "扫码入库（AC-4.2.1）")
    public Result<ScanResponse> scanInbound(
            @RequestBody ScanInboundRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return scanService.scanInbound(req, userId);
    }

    @PostMapping("/outbound")
    @Operation(summary = "扫码出库（AC-4.2.2）")
    public Result<ScanResponse> scanOutbound(
            @RequestBody ScanOutboundRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return scanService.scanOutbound(req, userId);
    }

    @PostMapping("/sync")
    @Operation(summary = "离线缓存批量同步（AC-4.2.3 · TTL 24h）")
    public Result<List<ScanResponse>> syncOffline(
            @RequestBody ScanOfflineSyncRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return scanService.syncOffline(req, userId);
    }

    @GetMapping("/list")
    @Operation(summary = "扫码历史查询")
    public Result<Map<String, Object>> listScans(ScanQueryRequest query) {
        return scanService.listScans(query);
    }

    @GetMapping("/locations")
    @Operation(summary = "库位列表")
    public Result<List<CrmWarehouseLocation>> listLocations(
            @RequestParam(required = false) String warehouse) {
        return scanService.listLocations(warehouse);
    }

    @GetMapping("/locations/recommend")
    @Operation(summary = "库位推荐（P2 修补 3）")
    public Result<String> recommendLocation(@RequestParam String materialCode) {
        return scanService.recommendLocation(materialCode);
    }

    @GetMapping("/inventory-sync")
    @Operation(summary = "库存同步（AC-4.2.4 · 增量）")
    public Result<Map<String, Object>> syncInventory(
            @RequestParam String clientId,
            @RequestParam(required = false) Long lastSyncedAt) {
        return scanService.syncInventory(clientId, lastSyncedAt);
    }

    @GetMapping("/history/{barcodeNo}")
    @Operation(summary = "扫码追溯")
    public Result<List<CrmWarehouseScan>> getScanHistory(@PathVariable String barcodeNo) {
        return scanService.getScanHistory(barcodeNo);
    }
}
