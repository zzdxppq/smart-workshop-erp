package com.btsheng.erp.business.crm.quote.controller;

import com.btsheng.erp.business.crm.quote.dto.QuoteCreateRequest;
import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.service.OrderConversionService;
import com.btsheng.erp.business.crm.quote.service.PdfExportService;
import com.btsheng.erp.business.crm.quote.service.QuoteApprovalService;
import com.btsheng.erp.business.crm.quote.service.QuoteEmailService;
import com.btsheng.erp.business.crm.quote.service.QuoteService;
import com.btsheng.erp.business.crm.quote.service.QuoteProcessService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.SalesDataScopeHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.5 · AC-2.2.1/2/3 · 报价 8 端点
 * V2.1 改造：新增工程师工艺填写端点
 */
@Tag(name = "E2-Quote", description = "报价与多级审批")
@RestController
@RequestMapping("/quotes")
public class QuoteController {

    private final QuoteService quoteService;
    private final QuoteApprovalService approvalService;
    private final OrderConversionService conversionService;
    private final PdfExportService exportService;
    private final QuoteProcessService processService;
    private final QuoteEmailService quoteEmailService;

    @Autowired
    public QuoteController(QuoteService quoteService, QuoteApprovalService approvalService,
                          OrderConversionService conversionService, PdfExportService exportService,
                          QuoteProcessService processService, QuoteEmailService quoteEmailService) {
        this.quoteService = quoteService;
        this.approvalService = approvalService;
        this.conversionService = conversionService;
        this.exportService = exportService;
        this.processService = processService;
        this.quoteEmailService = quoteEmailService;
    }

    @Operation(summary = "创建报价")
    @PostMapping
    public Result<CrmQuote> create(@RequestBody QuoteCreateRequest req) {
        long operatorId = SalesDataScopeHelper.requireOperatorUserId(1L);
        if (req.getQuote() != null && req.getQuote().getOwnerUserId() == null) {
            req.getQuote().setOwnerUserId(operatorId);
        }
        return quoteService.createQuote(req.getQuote(), req.getItems(), operatorId);
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable("id") Long id) {
        Result<Map<String, Object>> detail = quoteService.getQuoteWithHistory(id);
        if (!detail.isSuccess() || detail.getData() == null) {
            return detail;
        }
        if (SalesDataScopeHelper.effectiveScope() != SalesDataScopeHelper.Scope.ALL) {
            Object q = detail.getData().get("quote");
            if (q instanceof CrmQuote quote) {
                Result<Void> scope = SalesDataScopeHelper.assertOwnerDept(quote.getOwnerUserId(), quote.getDeptId());
                if (!scope.isSuccess()) {
                    return Result.fail(scope.getCode(), scope.getMessage());
                }
            }
        }
        return detail;
    }

    @Operation(summary = "修改报价（仅 DRAFT）")
    @PutMapping("/{id}")
    public Result<CrmQuote> update(@PathVariable("id") Long id, @RequestBody CrmQuote quote) {
        Result<CrmQuote> existing = quoteService.getQuote(id);
        if (!existing.isSuccess() || existing.getData() == null) {
            return existing;
        }
        Result<Void> scope = SalesDataScopeHelper.assertOwnerDept(
                existing.getData().getOwnerUserId(), existing.getData().getDeptId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        return quoteService.updateQuote(id, quote, SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    @Operation(summary = "保存草稿（含明细行，V2.1）")
    @PutMapping("/{id}/draft")
    public Result<CrmQuote> saveDraft(@PathVariable("id") Long id, @RequestBody QuoteCreateRequest req) {
        return quoteService.saveDraftWithItems(id, req.getQuote(), req.getItems(),
                SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    @Operation(summary = "提交给工程师（DRAFT → PENDING_ENG，V2.1）")
    @PostMapping("/{id}/submit-to-engineer")
    public Result<CrmQuote> submitToEngineer(@PathVariable("id") Long id) {
        return approvalService.submitToEngineer(id, SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    @Operation(summary = "列表查询")
    @GetMapping
    public Result<List<CrmQuote>> list(@RequestParam(value = "page", required = false) Integer page,
                                       @RequestParam(value = "size", required = false) Integer size,
                                       @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                       @RequestParam(value = "status", required = false) String status,
                                       @RequestParam(value = "customerId", required = false) Long customerId,
                                       @RequestParam(value = "owner", required = false) Long owner) {
        int p = pageNum != null ? pageNum : (page != null ? page : 1);
        int s = pageSize != null ? pageSize : (size != null ? size : 20);
        Long scopedOwner = SalesDataScopeHelper.resolveOwnerUserId(owner);
        Long scopedDept = SalesDataScopeHelper.resolveDeptId(null);
        return quoteService.listQuotes(p, s, status, customerId, scopedOwner, scopedDept);
    }

    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    public Result<CrmQuote> submit(@PathVariable("id") Long id) {
        Result<CrmQuote> existing = quoteService.getQuote(id);
        if (!existing.isSuccess() || existing.getData() == null) {
            return existing;
        }
        Result<Void> scope = SalesDataScopeHelper.assertOwnerDept(
                existing.getData().getOwnerUserId(), existing.getData().getDeptId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        return approvalService.submit(id, SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    public Result<CrmQuote> approve(@PathVariable("id") Long id) {
        if (!SalesDataScopeHelper.canApproveQuotes()) {
            return Result.fail(40303, "无审批权限");
        }
        return approvalService.approve(id, SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    @Operation(summary = "驳回")
    @PostMapping("/{id}/reject")
    public Result<CrmQuote> reject(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        if (!SalesDataScopeHelper.canApproveQuotes()) {
            return Result.fail(40303, "无审批权限");
        }
        return approvalService.reject(id, body.getOrDefault("reason", "未说明原因"),
                SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    @Operation(summary = "转订单")
    @PostMapping("/{id}/convert-to-order")
    public Result<Map<String, Object>> convertToOrder(@PathVariable("id") Long id) {
        return conversionService.convertToOrder(id, SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    @Operation(summary = "导出 PDF/Excel")
    @GetMapping("/export/{id}")
    public Result<byte[]> export(@PathVariable("id") Long id,
                                 @RequestParam(value = "format", defaultValue = "pdf") String format) {
        long operatorId = SalesDataScopeHelper.requireOperatorUserId(1L);
        if ("excel".equalsIgnoreCase(format)) return exportService.exportExcel(id, operatorId);
        return exportService.exportPdf(id, operatorId);
    }

    @Operation(summary = "发送报价 PDF 至客户邮箱")
    @PostMapping("/{id}/send-email")
    public Result<Map<String, Object>> sendEmail(@PathVariable("id") Long id) {
        return quoteEmailService.sendToCustomer(id, SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    // ========== V2.1 工程师工艺填写端点 ==========

    @Operation(summary = "工程师填写报价明细的工艺（V2.1）")
    @PostMapping("/items/{itemId}/process")
    public Result<?> fillProcess(@PathVariable("itemId") Long itemId, @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> processes = (List<Map<String, Object>>) request.get("processes");
        List<QuoteProcessService.ProcessDetail> processDetails = processes.stream().map(p -> {
            QuoteProcessService.ProcessDetail detail = new QuoteProcessService.ProcessDetail();
            detail.setProcessCode((String) p.get("processCode"));
            detail.setProcessName((String) p.get("processName"));
            detail.setMachineType((String) p.get("machineType"));
            if (p.get("unitTimeMinutes") != null) {
                detail.setUnitTimeMinutes(Integer.parseInt(p.get("unitTimeMinutes").toString()));
            }
            if (p.get("costPerHour") != null) {
                detail.setCostPerHour(new java.math.BigDecimal(p.get("costPerHour").toString()));
            }
            detail.setOutsourceFlag((Integer) p.getOrDefault("outsourceFlag", 0));
            detail.setRemark((String) p.get("remark"));
            return detail;
        }).toList();
        return processService.fillProcess(itemId, processDetails, SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    @Operation(summary = "保存表处面积（V2.1）")
    @PostMapping("/items/{itemId}/surface-areas")
    public Result<?> saveSurfaceAreas(@PathVariable("itemId") Long itemId, @RequestBody Map<String, Object> request) {
        java.math.BigDecimal anodize = request.get("anodizeArea") != null
                ? new java.math.BigDecimal(request.get("anodizeArea").toString()) : null;
        java.math.BigDecimal solid = request.get("solidSolutionArea") != null
                ? new java.math.BigDecimal(request.get("solidSolutionArea").toString()) : null;
        java.math.BigDecimal forming = request.get("formingArea") != null
                ? new java.math.BigDecimal(request.get("formingArea").toString()) : null;
        return processService.saveSurfaceAreas(itemId, anodize, solid, forming,
                SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    @Operation(summary = "获取报价明细的工艺信息（V2.1）")
    @GetMapping("/items/{itemId}/process")
    public Result<Map<String, Object>> getProcessInfo(@PathVariable("itemId") Long itemId) {
        return processService.getProcessInfo(itemId);
    }

    @Operation(summary = "根据范本自动填充工艺（V2.1）")
    @PostMapping("/items/{itemId}/apply-template/{templateId}")
    public Result<?> applyTemplate(@PathVariable("itemId") Long itemId, @PathVariable("templateId") Long templateId) {
        return processService.applyTemplate(itemId, templateId, SalesDataScopeHelper.requireOperatorUserId(1L));
    }

    @Operation(summary = "自动计算报价（工程师填写工艺后，V2.1）")
    @PostMapping("/items/{itemId}/calculate")
    public Result<Map<String, Object>> calculateQuoteItem(@PathVariable("itemId") Long itemId) {
        return processService.calculateQuoteItem(itemId, SalesDataScopeHelper.requireOperatorUserId(1L));
    }
}
