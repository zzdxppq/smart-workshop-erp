package com.btsheng.erp.business.crm.purchaseorder.controller;

import com.btsheng.erp.business.crm.purchaseorder.dto.CreatePoRequest;
import com.btsheng.erp.business.crm.purchaseorder.service.PurchaseOrderService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/po")
@Tag(name = "E8-PO", description = "采购订单（Story 1.24）")
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @Autowired
    public PurchaseOrderController(PurchaseOrderService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "采购单列表")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.list(keyword, status, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "采购单详情")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return service.getDetail(id);
    }

    @PostMapping
    @Operation(summary = "创建采购单")
    public Result<Map<String, Object>> create(
            @RequestBody CreatePoRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.create(req, userId);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认采购单")
    public Result<Map<String, Object>> confirm(@PathVariable Long id) {
        return service.confirm(id);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭采购单")
    public Result<Map<String, Object>> close(@PathVariable Long id) {
        return service.close(id);
    }
}
