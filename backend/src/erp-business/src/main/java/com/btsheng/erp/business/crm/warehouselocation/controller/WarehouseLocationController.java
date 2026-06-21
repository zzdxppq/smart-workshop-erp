package com.btsheng.erp.business.crm.warehouselocation.controller;

import com.btsheng.erp.business.crm.warehouselocation.dto.BatchCreateRequest;
import com.btsheng.erp.business.crm.warehouselocation.dto.BatchTraceResponse;
import com.btsheng.erp.business.crm.warehouselocation.dto.LocationCreateRequest;
import com.btsheng.erp.business.crm.warehouselocation.dto.LocationUpdateRequest;
import com.btsheng.erp.business.crm.warehouselocation.dto.StocktakeCreateRequest;
import com.btsheng.erp.business.crm.warehouselocation.dto.WarehouseCreateRequest;
import com.btsheng.erp.business.crm.warehouselocation.dto.WarehouseUpdateRequest;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmBatch;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmWarehouse;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmWarehouseLocationExt;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmWarehouseStocktake;
import com.btsheng.erp.business.crm.warehouselocation.service.StocktakeService;
import com.btsheng.erp.business.crm.warehouselocation.service.WarehouseLocationService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.13 · 库位批次与多仓库 Controller
 *
 * 6 端点：
 * - POST /warehouses                     创建仓库（AC-4.3.1）
 * - POST /warehouses/locations           创建库位（AC-4.3.1）
 * - GET  /warehouses/locations/tree      库位树
 * - POST /warehouses/batches             创建批次（AC-4.3.2）
 * - GET  /warehouses/batches/{batchNo}   批次追溯（AC-4.3.2）
 * - GET  /warehouses                     仓库列表（AC-4.3.3）
 */
@RestController
@RequestMapping("/warehouses")
@Tag(name = "E4-Location", description = "库位批次与多仓库（Story 1.13）")
public class WarehouseLocationController {

    private final WarehouseLocationService locationService;
    private final StocktakeService stocktakeService;

    @Autowired
    public WarehouseLocationController(WarehouseLocationService locationService,
                                       StocktakeService stocktakeService) {
        this.locationService = locationService;
        this.stocktakeService = stocktakeService;
    }

    @PostMapping
    @Operation(summary = "创建仓库（AC-4.3.1）")
    public Result<CrmWarehouse> createWarehouse(
            @RequestBody WarehouseCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return locationService.createWarehouse(req, userId);
    }

    @PostMapping("/locations")
    @Operation(summary = "创建库位（AC-4.3.1 · LOC-A01-01-01）")
    public Result<CrmWarehouseLocationExt> createLocation(
            @RequestBody LocationCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return locationService.createLocation(req, userId);
    }

    @GetMapping("/locations/tree")
    @Operation(summary = "库位树 3 级（仓库 / 库区 / 库位）")
    public Result<List<Map<String, Object>>> getLocationTree() {
        return locationService.getLocationTree();
    }

    @PostMapping("/batches")
    @Operation(summary = "创建批次（AC-4.3.2 · BATCH{yyyyMMdd}{seq:4}）")
    public Result<CrmBatch> createBatch(
            @RequestBody BatchCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return locationService.createBatch(req, userId);
    }

    @GetMapping("/batches/{batchNo}")
    @Operation(summary = "批次追溯（AC-4.3.2）")
    public Result<BatchTraceResponse> getBatchTrace(@PathVariable String batchNo) {
        return locationService.getBatchTrace(batchNo);
    }

    @GetMapping
    @Operation(summary = "仓库列表（AC-4.3.3）")
    public Result<List<CrmWarehouse>> listWarehouses() {
        return locationService.listWarehouses();
    }

    @PutMapping("/{warehouseCode}")
    @Operation(summary = "更新仓库")
    public Result<CrmWarehouse> updateWarehouse(
            @PathVariable String warehouseCode,
            @RequestBody WarehouseUpdateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return locationService.updateWarehouse(warehouseCode, req, userId);
    }

    @PutMapping("/locations/{locationCode}")
    @Operation(summary = "更新库位")
    public Result<CrmWarehouseLocationExt> updateLocation(
            @PathVariable String locationCode,
            @RequestBody LocationUpdateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return locationService.updateLocation(locationCode, req, userId);
    }

    @GetMapping("/batches")
    @Operation(summary = "批次列表（FEFO 排序）")
    public Result<List<CrmBatch>> listBatches(
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) String qualityStatus) {
        return locationService.listBatches(materialCode, qualityStatus);
    }

    @GetMapping("/utilization")
    @Operation(summary = "库位利用率统计（P2 修补 3）")
    public Result<List<Map<String, Object>>> utilization() {
        return locationService.warehouseUtilization();
    }

    @GetMapping("/accessible")
    @Operation(summary = "按用户权限列出可见仓库（P1 修补 3 · 多仓库权限隔离）")
    public Result<List<CrmWarehouse>> listAccessible(
            @RequestParam(required = false) Long userId) {
        return locationService.listAccessibleWarehouses(userId);
    }

    @GetMapping("/stocktake")
    @Operation(summary = "盘点单列表")
    public Result<Map<String, Object>> listStocktakes() {
        return stocktakeService.listStocktakes();
    }

    @PostMapping("/stocktake")
    @Operation(summary = "新建盘点单")
    public Result<CrmWarehouseStocktake> createStocktake(
            @RequestBody StocktakeCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return stocktakeService.createStocktake(req, userId);
    }

    @Operation(summary = "待入库列表（V1.3.9 补全）")
    @GetMapping("/inbound/pending")
    public Result<Map<String, Object>> listInboundPending(
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> items = locationService.listInboundPending(size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", items);
        return Result.ok(result);
    }
}
