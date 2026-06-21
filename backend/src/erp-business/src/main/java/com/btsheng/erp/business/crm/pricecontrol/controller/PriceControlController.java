package com.btsheng.erp.business.crm.pricecontrol.controller;

import com.btsheng.erp.business.crm.pricecontrol.dto.CheckPriceRequest;
import com.btsheng.erp.business.crm.pricecontrol.dto.SetPriceLimitRequest;
import com.btsheng.erp.business.crm.pricecontrol.entity.CrmPriceControl;
import com.btsheng.erp.business.crm.pricecontrol.entity.CrmPriceHistory;
import com.btsheng.erp.business.crm.pricecontrol.service.PriceControlService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.33 · 采购·价格控制 Controller (FR-8-2)
 *
 * <p>3 端点：
 * <ul>
 *   <li>POST /price-control/limit       设置物料限价（AC-8.2.1）</li>
 *   <li>POST /price-control/check       价格校验（AC-8.2.2 · 偏差告警）</li>
 *   <li>GET  /price-control/history     历史价（3 月内）</li>
 *   <li>GET  /price-control/limit       查询物料限价</li>
 * </ul>
 */
@RestController
@RequestMapping("/price-control")
@Tag(name = "E8-Price-Control", description = "采购·价格控制（Story 1.33 FR-8-2）")
public class PriceControlController {

    private final PriceControlService service;

    @Autowired
    public PriceControlController(PriceControlService service) {
        this.service = service;
    }

    @PostMapping("/limit")
    @Operation(summary = "设置物料限价（AC-8.2.1）")
    public Result<CrmPriceControl> setLimit(
            @RequestBody SetPriceLimitRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Name", defaultValue = "system") String userName) {
        return service.setPriceLimit(req, userId, userName);
    }

    @GetMapping("/limit")
    @Operation(summary = "查询物料限价（厂商专享 > 通用）")
    public Result<CrmPriceControl> getLimit(
            @RequestParam Long materialId,
            @RequestParam(required = false) Long vendorId) {
        return service.getPriceLimit(materialId, vendorId);
    }

    @PostMapping("/check")
    @Operation(summary = "价格校验（AC-8.2.2 · 偏差率 ≥ 20% ALERTED）")
    public Result<Map<String, Object>> check(@RequestBody CheckPriceRequest req) {
        return service.checkPrice(req);
    }

    @GetMapping("/history")
    @Operation(summary = "历史价（3 月内）")
    public Result<List<CrmPriceHistory>> history(
            @RequestParam Long materialId,
            @RequestParam(required = false) Long vendorId) {
        return service.listPriceHistory(materialId, vendorId);
    }
}
