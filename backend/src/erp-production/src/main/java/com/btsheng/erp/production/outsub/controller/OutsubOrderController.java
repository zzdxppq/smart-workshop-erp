package com.btsheng.erp.production.outsub.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsub.dto.OutsubOrderCreateRequest;
import com.btsheng.erp.production.outsub.service.OutsubOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * V1.3.7 · E6-Outsub · 采购选厂商创建 WW- 单
 */
@RestController
@RequestMapping("/outsub/orders")
@Tag(name = "E6-Outsub", description = "委外下单（采购专用 · V1.3.7 AD-1）")
public class OutsubOrderController {

    private final OutsubOrderService outsubOrderService;

    @Autowired
    public OutsubOrderController(OutsubOrderService outsubOrderService) {
        this.outsubOrderService = outsubOrderService;
    }

    @PostMapping
    @Operation(summary = "【采购】选厂商创建 WW- 单（含 drawingId 图纸关联）")
    public Result<CrmOutsourceOrder> create(
            @RequestBody OutsubOrderCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return outsubOrderService.createOrder(req, userId);
    }
}
