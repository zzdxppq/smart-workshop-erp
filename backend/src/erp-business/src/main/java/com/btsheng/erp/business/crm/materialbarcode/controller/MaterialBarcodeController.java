package com.btsheng.erp.business.crm.materialbarcode.controller;

import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeBatchGenerateRequest;
import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeGenerateRequest;
import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeQueryRequest;
import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeResponse;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterialBarcode;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterialCategory;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.materialbarcode.service.MaterialBarcodeService;
import com.btsheng.erp.business.crm.materialbarcodebatch.dto.MaterialBarcodeGenerateRequest;
import com.btsheng.erp.business.crm.materialbarcodebatch.dto.MaterialBarcodeGenerateResponse;
import com.btsheng.erp.business.crm.materialbarcodebatch.service.MaterialBarcodeBatchService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.11 · 物料条码生成 Controller
 *
 * 4 端点：
 * - POST /materials/barcode/generate  物料条码生成（AC-4.1.1）
 * - POST /materials/barcode/batch-generate  批量条码（AC-4.1.2）
 * - GET  /materials/barcode/{barcodeNo}    扫码解析（AC-4.1.3）
 * - GET  /materials/barcodes              列表查询
 */
@RestController
@RequestMapping("/materials")
@Tag(name = "E4-Barcode", description = "物料条码生成（Story 1.11）")
public class MaterialBarcodeController {

    private final MaterialBarcodeService barcodeService;
    private final MaterialBarcodeBatchService barcodeBatchService;
    private final CrmMaterialMapper materialMapper;

    @Autowired
    public MaterialBarcodeController(MaterialBarcodeService barcodeService,
                                      MaterialBarcodeBatchService barcodeBatchService,
                                      CrmMaterialMapper materialMapper) {
        this.barcodeService = barcodeService;
        this.barcodeBatchService = barcodeBatchService;
        this.materialMapper = materialMapper;
    }

    @PostMapping("/barcode/generate")
    @Operation(summary = "物料条码生成（AC-4.1.1）")
    public Result<BarcodeResponse> generateBarcode(
            @RequestBody BarcodeGenerateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long operatorUserId) {
        return barcodeService.generateBarcode(req, operatorUserId);
    }

    @PostMapping("/barcode/batch-generate")
    @Operation(summary = "批量条码生成（AC-4.1.2 · P1 修补 100 并发不重复）")
    public Result<List<BarcodeResponse>> batchGenerateBarcodes(
            @RequestBody BarcodeBatchGenerateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long operatorUserId) {
        return barcodeService.batchGenerateBarcodes(req, operatorUserId);
    }

    @PostMapping("/barcode-batch/generate")
    @Operation(summary = "复合物料码生成（Story 3.2 · 入库单别名路径）")
    public Result<MaterialBarcodeGenerateResponse> generateCompositeBarcode(
            @Valid @RequestBody MaterialBarcodeGenerateRequest req) {
        return barcodeBatchService.generateBarcode(req);
    }

    @GetMapping("/barcode/{barcodeNo}")
    @Operation(summary = "扫码解析（AC-4.1.3）")
    public Result<BarcodeResponse> parseBarcode(
            @Parameter(description = "条码号 BC{yyyyMMdd}{seq:4}") @PathVariable String barcodeNo,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long operatorUserId) {
        return barcodeService.parseBarcode(barcodeNo, operatorUserId);
    }

    @GetMapping("/barcodes")
    @Operation(summary = "物料条码列表查询")
    public Result<Map<String, Object>> listBarcodes(BarcodeQueryRequest query) {
        return barcodeService.listBarcodes(query);
    }

    @PostMapping("/barcode/{barcodeNo}/regenerate")
    @Operation(summary = "重新生成条码（条码丢失/损坏）")
    public Result<BarcodeResponse> regenerateBarcode(
            @PathVariable String barcodeNo,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long operatorUserId) {
        return barcodeService.regenerateBarcode(barcodeNo, operatorUserId);
    }

    @GetMapping("/barcode/categories")
    @Operation(summary = "物料分类列表（5 段聚合 · P2 修补 3）")
    public Result<List<CrmMaterialCategory>> listCategories() {
        return barcodeService.listCategories();
    }

    @GetMapping("/barcode/by-material/{materialCode}")
    @Operation(summary = "按物料编码列出条码")
    public Result<List<CrmMaterialBarcode>> listByMaterial(@PathVariable String materialCode) {
        return barcodeService.listBarcodesByMaterial(materialCode);
    }

    @GetMapping("/barcode/materials")
    @Operation(summary = "物料主数据列表（用于下拉）")
    public Result<List<CrmMaterial>> listMaterials() {
        return Result.ok(materialMapper.selectAllActive());
    }

    @GetMapping
    @Operation(summary = "物料主数据列表（Web · 支持 categoryPrefix 过滤）")
    public Result<Map<String, Object>> listMaterialsForWeb(
            @RequestParam(required = false) String categoryPrefix,
            @RequestParam(defaultValue = "50") int size) {
        if (size < 1 || size > 500) {
            size = 50;
        }
        List<CrmMaterial> rows;
        if (categoryPrefix != null && !categoryPrefix.isBlank()) {
            rows = materialMapper.selectByCodePrefix(categoryPrefix.trim(), size);
        } else {
            rows = materialMapper.selectAllActive();
            if (rows != null && rows.size() > size) {
                rows = rows.subList(0, size);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("items", rows == null ? List.of() : rows);
        data.put("list", rows == null ? List.of() : rows);
        data.put("total", rows == null ? 0 : rows.size());
        return Result.ok(data);
    }
}
