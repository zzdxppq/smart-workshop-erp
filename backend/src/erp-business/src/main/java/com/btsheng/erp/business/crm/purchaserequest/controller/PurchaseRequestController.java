package com.btsheng.erp.business.crm.purchaserequest.controller;

import com.btsheng.erp.business.crm.purchaserequest.dto.ConvertPrToPoRequest;
import com.btsheng.erp.business.crm.purchaserequest.service.PurchaseRequestService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/purchase-request")
@Tag(name = "E5-PR", description = "采购申请 · MRP 缺料转单")
public class PurchaseRequestController {

    private final PurchaseRequestService service;

    @Autowired
    public PurchaseRequestController(PurchaseRequestService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "采购申请列表")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.list(status, keyword, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "采购申请详情")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return service.getDetail(id);
    }

    @PostMapping("/{id}/convert-to-po")
    @Operation(summary = "转采购单（物料/数量由 PR 锁定）")
    public Result<Map<String, Object>> convertToPo(
            @PathVariable Long id,
            @RequestBody ConvertPrToPoRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.convertToPo(id, req, userId);
    }
}
