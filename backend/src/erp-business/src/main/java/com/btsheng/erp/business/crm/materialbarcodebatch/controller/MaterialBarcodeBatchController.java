package com.btsheng.erp.business.crm.materialbarcodebatch.controller;

import com.btsheng.erp.business.crm.materialbarcodebatch.dto.MaterialBarcodeGenerateRequest;
import com.btsheng.erp.business.crm.materialbarcodebatch.dto.MaterialBarcodeGenerateResponse;
import com.btsheng.erp.business.crm.materialbarcodebatch.dto.MaterialBarcodeParseResponse;
import com.btsheng.erp.business.crm.materialbarcodebatch.service.MaterialBarcodeBatchService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * V1.3.8 · Story 3.2 · 物料码批次 Controller
 *
 * <p>2 端点（与 Story 3.2 端点契约一致）：
 * <ul>
 *   <li>POST /material-barcode/generate  内部接口（3.1 调用）</li>
 *   <li>GET  /material-barcode/parse     APP 扫码解析</li>
 * </ul>
 *
 * <p>与 V1.3.7 1.11 老 MaterialBarcodeController（/materials/barcode/...）共存不冲突。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@RestController
@RequestMapping("/material-barcode")
@Tag(name = "V1.3.8-Story3.2-物料码批次")
public class MaterialBarcodeBatchController {

    private final MaterialBarcodeBatchService service;

    @Autowired
    public MaterialBarcodeBatchController(MaterialBarcodeBatchService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    @Operation(summary = "生成复合物料码（Story 3.2 AC-3.2.1 · 内部接口）")
    public Result<MaterialBarcodeGenerateResponse> generate(@Valid @RequestBody MaterialBarcodeGenerateRequest req) {
        return service.generateBarcode(req);
    }

    @GetMapping("/parse")
    @Operation(summary = "物料码解析（Story 3.2 AC-3.2.2 · APP 扫码）")
    public Result<MaterialBarcodeParseResponse> parse(@RequestParam("barcode") String barcode) {
        return service.parseBarcode(barcode);
    }
}