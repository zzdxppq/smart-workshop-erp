package com.btsheng.erp.production.outsource.controller;

import com.btsheng.erp.core.integration.dto.OutsourceHistoryPriceResult;
import com.btsheng.erp.core.integration.dto.OutsourceOrderRef;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.production.outsource.dto.OutsourceCreateRequest;
import com.btsheng.erp.production.outsource.dto.OutsourceQueryRequest;
import com.btsheng.erp.production.outsource.dto.OutsourceSubmitRequest;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceHistory;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceItem;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.service.OutsourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/outsource")
@Tag(name = "E5-Outsource", description = "委外下单基础（Story 1.18）")
public class OutsourceController {

    private final OutsourceService service;

    @Autowired
    public OutsourceController(OutsourceService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建委外单（AC-5.4.2 · WW{yyyyMMdd}{seq:4}）")
    public Result<CrmOutsourceOrder> createOutsource(
            @RequestBody OutsourceCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createOutsourceOrder(req, userId);
    }

    @PostMapping("/{outsourceNo}/submit")
    @Operation(summary = "提交委外单（AC-5.4.3 · 1.2 审批流）")
    public Result<CrmOutsourceOrder> submitOutsource(
            @PathVariable String outsourceNo,
            @RequestBody(required = false) OutsourceSubmitRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        if (req == null) {
            req = new OutsourceSubmitRequest();
        }
        return service.submitOutsource(outsourceNo, req, userId);
    }

    @PostMapping("/{outsourceNo}/accept")
    @Operation(summary = "供应商接单")
    public Result<CrmOutsourceOrder> acceptOutsource(
            @PathVariable String outsourceNo,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.acceptOutsource(outsourceNo, userId);
    }

    @PostMapping("/{outsourceNo}/complete")
    @Operation(summary = "委外完工")
    public Result<CrmOutsourceOrder> completeOutsource(
            @PathVariable String outsourceNo,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.completeOutsource(outsourceNo, userId);
    }

    @PostMapping("/{outsourceNo}/close")
    @Operation(summary = "关闭委外单")
    public Result<CrmOutsourceOrder> closeOutsource(
            @PathVariable String outsourceNo,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.closeOutsource(outsourceNo, userId);
    }

    @PostMapping("/{outsourceNo}/rework")
    @Operation(summary = "返修（P1 修补 4 · 返修次数 ≤ 3）")
    public Result<CrmOutsourceOrder> reworkOutsource(
            @PathVariable String outsourceNo,
            @RequestParam(required = false) String note,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.reworkOutsource(outsourceNo, note, userId);
    }

    @GetMapping
    @Operation(summary = "委外列表")
    public Result<Map<String, Object>> list(OutsourceQueryRequest query) {
        return service.listOutsourceOrders(query);
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "按 ID 查询委外单摘要（跨服务）")
    public Result<OutsourceOrderRef> getById(@PathVariable Long id) {
        return service.getOutsourceOrderById(id);
    }

    @GetMapping("/history-price")
    @Operation(summary = "FR-6-3 委外历史价（vendorId + processName，最近 3 次中位数）")
    public Result<OutsourceHistoryPriceResult> historyPrice(
            @RequestParam Long vendorId,
            @RequestParam String processName) {
        return service.getHistoryPrice(vendorId, processName);
    }

    @GetMapping("/price-history")
    @Operation(summary = "价格历史调取（按物料编码，兼容旧接口）")
    public Result<List<BigDecimal>> priceHistory(
            @RequestParam Long supplierId,
            @RequestParam String materialCode) {
        return service.getPriceHistory(supplierId, materialCode);
    }

    @GetMapping("/price-suggest")
    @Operation(summary = "委外价格建议（FR-6-3）")
    public Result<OutsourceHistoryPriceResult> priceSuggest(
            @RequestParam Long supplierId,
            @RequestParam(required = false) String processName,
            @RequestParam(required = false) String materialCode) {
        return service.getPriceSuggest(supplierId, processName, materialCode);
    }

    @GetMapping("/{outsourceNo}")
    @Operation(summary = "委外详情")
    public Result<CrmOutsourceOrder> get(@PathVariable String outsourceNo) {
        return service.getOutsourceOrder(outsourceNo);
    }

    @GetMapping("/{outsourceNo}/items")
    @Operation(summary = "委外明细")
    public Result<List<CrmOutsourceItem>> items(@PathVariable String outsourceNo) {
        return service.listItems(outsourceNo);
    }

    @GetMapping("/{outsourceNo}/history")
    @Operation(summary = "委外历史")
    public Result<List<CrmOutsourceHistory>> history(@PathVariable String outsourceNo) {
        return service.listHistory(outsourceNo);
    }
}
