package com.btsheng.erp.business.internal.productroute.controller;

import com.btsheng.erp.business.internal.productroute.dto.ProductRouteMaterialDto;
import com.btsheng.erp.business.internal.productroute.dto.ProductRouteRowDto;
import com.btsheng.erp.business.internal.productroute.dto.UpdateMaterialProcessRequest;
import com.btsheng.erp.business.internal.productroute.entity.MdmProcess;
import com.btsheng.erp.business.internal.productroute.service.ProductRouteMaterialInternalService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内部 API · erp-production → erp-business（cnc_business 物料/工艺路线，替代 production 双数据源）
 */
@RestController
@RequestMapping("/internal/product-route")
@Tag(name = "Internal-ProductRoute", description = "产品工艺路线 · business 库内部接口")
public class ProductRouteMaterialInternalController {

    private final ProductRouteMaterialInternalService service;

    public ProductRouteMaterialInternalController(ProductRouteMaterialInternalService service) {
        this.service = service;
    }

    @GetMapping("/materials/{productId}")
    @Operation(summary = "解析物料（id 或 material_code）")
    public Result<ProductRouteMaterialDto> resolveMaterial(@PathVariable String productId) {
        return service.resolveMaterial(productId);
    }

    @PutMapping("/materials/{materialId}/process-id")
    @Operation(summary = "回写物料 process_id")
    public Result<Void> updateProcessId(@PathVariable Long materialId,
                                        @RequestBody UpdateMaterialProcessRequest req) {
        return service.updateMaterialProcessId(materialId, req);
    }

    @GetMapping("/mdm-process/{processCode}")
    @Operation(summary = "按编码查 MDM 工序")
    public Result<MdmProcess> getMdmProcess(@PathVariable String processCode) {
        return service.getMdmProcess(processCode);
    }

    @GetMapping("/routes/{productCode}")
    @Operation(summary = "产品工艺路线列表")
    public Result<List<ProductRouteRowDto>> listRoutes(@PathVariable String productCode) {
        return service.listProductRoutes(productCode);
    }

    @PutMapping("/routes/{productCode}")
    @Operation(summary = "全量替换产品工艺路线")
    public Result<Void> replaceRoutes(@PathVariable String productCode,
                                      @RequestBody List<ProductRouteRowDto> routes) {
        return service.replaceProductRoutes(productCode, routes);
    }
}
