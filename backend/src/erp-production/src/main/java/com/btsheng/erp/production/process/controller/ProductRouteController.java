package com.btsheng.erp.production.process.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.production.process.dto.ProductRouteCreateRequest;
import com.btsheng.erp.production.process.service.ProductRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** E3-S3.4 · 产品工艺路线 API */
@Tag(name = "E3-Product-Route", description = "产品工艺路线（Story 3.4）")
@RestController
@RequestMapping("/products/{productId}/routes")
public class ProductRouteController {

    private final ProductRouteService productRouteService;

    @Autowired
    public ProductRouteController(ProductRouteService productRouteService) {
        this.productRouteService = productRouteService;
    }

    @PostMapping
    @Operation(summary = "创建/覆盖产品工艺路线")
    public Result<Map<String, Object>> create(
            @PathVariable String productId,
            @RequestBody ProductRouteCreateRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        return productRouteService.createProductRoute(productId, req, operatorUserId == null ? 0L : operatorUserId);
    }

    @GetMapping
    @Operation(summary = "查询产品工艺路线")
    public Result<Map<String, Object>> get(@PathVariable String productId) {
        return productRouteService.getProductRoute(productId);
    }

    @PostMapping("/copy-from/{srcProductId}")
    @Operation(summary = "从历史产品复制工艺路线")
    public Result<Map<String, Object>> copyFrom(
            @PathVariable String productId,
            @PathVariable String srcProductId,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        return productRouteService.copyProductRouteFrom(productId, srcProductId, operatorUserId == null ? 0L : operatorUserId);
    }

    @PostMapping("/publish")
    @Operation(summary = "发布产品工艺路线（DRAFT → RELEASED）")
    public Result<Map<String, Object>> publish(
            @PathVariable String productId,
            @RequestParam(value = "drawingId", required = false) Long drawingId,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        return productRouteService.publishProductRoute(productId, drawingId, operatorUserId == null ? 0L : operatorUserId);
    }
}
