package com.btsheng.erp.production.scan.controller;

import com.btsheng.erp.production.scan.dto.AppBarcodeReportRequest;
import com.btsheng.erp.production.scan.dto.AppBarcodeStartRequest;
import com.btsheng.erp.production.scan.dto.AppBarcodeTransferRequest;
import com.btsheng.erp.production.scan.dto.ScanReportRequest;
import com.btsheng.erp.production.scan.dto.ScanStartRequest;
import com.btsheng.erp.production.scan.dto.ScanStationRequest;
import com.btsheng.erp.production.scan.entity.CrmProductionReport;
import com.btsheng.erp.production.scan.entity.CrmProductionScan;
import com.btsheng.erp.production.scan.entity.CrmProductionStation;
import com.btsheng.erp.production.scan.service.ProductionScanService;
import com.btsheng.erp.production.scan.util.BarcodePrefixUtil;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * OpenAPI Spec 对齐端点（GD-/LZ-/SB-），委托 {@link ProductionScanService}。
 */
@RestController
@Tag(name = "E5-Scan-OpenAPI", description = "APP 扫码 OpenAPI 路径别名")
public class AppWorkorderScanController {

    private final ProductionScanService scanService;

    @Autowired
    public AppWorkorderScanController(ProductionScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping("/app/workorders/{barcode}/start")
    @Operation(summary = "扫码开工（OpenAPI · GD-）")
    public Result<CrmProductionScan> scanStartByBarcode(
            @PathVariable String barcode,
            @RequestBody(required = false) AppBarcodeStartRequest body,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        if (!BarcodePrefixUtil.isWorkorder(barcode)) {
            return Result.fail(40001, "INVALID_WORKORDER_BARCODE");
        }
        ScanStartRequest req = new ScanStartRequest();
        req.setWorkorderNo(BarcodePrefixUtil.normalizeWorkorderNo(barcode));
        req.setStepNo(body != null && body.getStepNo() != null ? body.getStepNo() : 1);
        req.setEquipmentId(body != null ? body.getEquipmentId() : null);
        req.setMachineBarcode(body != null ? body.getMachineBarcode() : null);
        return scanService.scanStart(req, userId);
    }

    @PostMapping("/app/workorders/{barcode}/report")
    @Operation(summary = "扫码报工（OpenAPI · qtyDone/qtyOk/qtyScrap）")
    public Result<CrmProductionReport> scanReportByBarcode(
            @PathVariable String barcode,
            @RequestBody AppBarcodeReportRequest body,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        if (!BarcodePrefixUtil.isWorkorder(barcode)) {
            return Result.fail(40001, "INVALID_WORKORDER_BARCODE");
        }
        if (body == null || body.getQtyOk() == null) {
            return Result.fail(40001, "QTY_OK_REQUIRED");
        }
        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo(BarcodePrefixUtil.normalizeWorkorderNo(barcode));
        req.setStepNo(body.getStepNo() != null ? body.getStepNo() : 1);
        req.setReportedQty(body.getQtyOk());
        return scanService.scanReport(req, userId);
    }

    @PostMapping("/app/transfer/{barcode}/next")
    @Operation(summary = "扫码过站（OpenAPI · LZ-）")
    public Result<CrmProductionStation> scanTransferByBarcode(
            @PathVariable String barcode,
            @RequestBody(required = false) AppBarcodeTransferRequest body,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        if (!BarcodePrefixUtil.isTransfer(barcode)) {
            return Result.fail(40001, "INVALID_TRANSFER_BARCODE");
        }
        ScanStationRequest req = new ScanStationRequest();
        req.setWorkorderNo(body != null && body.getWorkorderNo() != null
                ? BarcodePrefixUtil.normalizeWorkorderNo(body.getWorkorderNo())
                : null);
        if (req.getWorkorderNo() == null) {
            return Result.fail(40001, "WORKORDER_NO_REQUIRED");
        }
        req.setFromStepNo(body != null && body.getFromStepNo() != null ? body.getFromStepNo() : 1);
        req.setToStepNo(body != null && body.getToStepNo() != null ? body.getToStepNo() : 2);
        return scanService.scanStationChange(req, userId);
    }
}
