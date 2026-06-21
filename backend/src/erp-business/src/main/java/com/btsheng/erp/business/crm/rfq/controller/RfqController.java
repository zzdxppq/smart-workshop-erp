package com.btsheng.erp.business.crm.rfq.controller;

import com.btsheng.erp.business.crm.rfq.dto.AddRfqVendorRequest;
import com.btsheng.erp.business.crm.rfq.dto.AwardRfqRequest;
import com.btsheng.erp.business.crm.rfq.dto.CreateRfqRequest;
import com.btsheng.erp.business.crm.rfq.dto.SubmitQuoteRequest;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfq;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfqQuote;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfqVendor;
import com.btsheng.erp.business.crm.rfq.service.RfqService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.32 · 采购·询比价 Controller (FR-8-1)
 *
 * <p>5 端点：
 * <ul>
 *   <li>POST /rfq                    创建询价单（AC-8.1.1）</li>
 *   <li>POST /rfq/{id}/vendor        添加询价厂商（≥ 3 厂商）</li>
 *   <li>POST /rfq/{id}/quote         厂商报价</li>
 *   <li>POST /rfq/{id}/compare       自动比价（最低/加权）</li>
 *   <li>POST /rfq/{id}/award         中标 + 自动触发 PO</li>
 *   <li>GET  /rfq                    列表</li>
 *   <li>GET  /rfq/{id}               详情（厂商+报价）</li>
 * </ul>
 */
@RestController
@RequestMapping("/rfq")
@Tag(name = "E8-RFQ", description = "采购·询比价（Story 1.32 FR-8-1）")
public class RfqController {

    private final RfqService service;

    @Autowired
    public RfqController(RfqService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建询价单（AC-8.1.1）")
    public Result<CrmRfq> create(
            @RequestBody CreateRfqRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createRfq(req, userId);
    }

    @PostMapping("/{id}/vendor")
    @Operation(summary = "添加询价厂商（≥ 3 厂商）")
    public Result<CrmRfqVendor> addVendor(
            @PathVariable Long id,
            @RequestBody AddRfqVendorRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.addVendor(id, req, userId);
    }

    @PostMapping("/{id}/quote")
    @Operation(summary = "厂商报价")
    public Result<CrmRfqQuote> submitQuote(
            @PathVariable Long id,
            @RequestBody SubmitQuoteRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.submitQuote(id, req, userId);
    }

    @GetMapping("/{id}/compare")
    @Operation(summary = "比价预览（只读，供 Web 比价页）")
    public Result<Map<String, Object>> comparePreview(@PathVariable Long id) {
        return service.previewCompare(id);
    }

    @PostMapping("/{id}/compare")
    @Operation(summary = "自动比价（最低价 / 加权评分 2 模式）")
    public Result<Map<String, Object>> compare(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.compareQuotes(id);
    }

    @PostMapping("/{id}/award")
    @Operation(summary = "中标 + 自动触发 PO 闭环")
    public Result<Map<String, Object>> award(
            @PathVariable Long id,
            @RequestBody(required = false) AwardRfqRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.awardRfq(id, req, userId);
    }

    @PostMapping("/{id}/convert-to-po")
    @Operation(summary = "定标后一键转采购单")
    public Result<Map<String, Object>> convertToPo(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.convertToPurchaseOrder(id, userId);
    }

    @PostMapping("/{id}/convert-to-outsource")
    @Operation(summary = "定标后一键转委外单")
    public Result<Map<String, Object>> convertToOutsource(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.convertToOutsourceOrder(id, userId);
    }

    @GetMapping
    @Operation(summary = "询价单列表")
    public Result<List<CrmRfq>> list(@RequestParam(required = false) String status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "询价单详情（厂商+报价）")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return service.getDetail(id);
    }
}
