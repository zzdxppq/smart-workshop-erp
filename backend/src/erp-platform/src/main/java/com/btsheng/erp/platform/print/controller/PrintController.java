package com.btsheng.erp.platform.print.controller;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.print.dto.PdfA4PrintRequest;
import com.btsheng.erp.platform.print.dto.PrintLogResponse;
import com.btsheng.erp.platform.print.dto.PrintStatisticsBucket;
import com.btsheng.erp.platform.print.dto.ReprintRequest;
import com.btsheng.erp.platform.print.dto.ZplPrintRequest;
import com.btsheng.erp.platform.print.dto.ZplPrintResult;
import com.btsheng.erp.platform.print.service.PrintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import com.btsheng.erp.core.web.PreAuthorizeRoles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 双模式打印 Controller（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.1~5）
 *
 * <p>6 端点：zpl / pdf-a4 / logs / logs/{id} / logs/{id}/replay / statistics
 * <p>权限：模式一二任意已登录用户 · logs/查询 admin + 操作人 · replay admin
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Tag(name = "E12-Print", description = "双模式打印（Story 12.4）· 模式一 ZPL/TSPL 直连 + 模式二 A4 PDF 浏览器")
@RestController
@RequestMapping("/print")
public class PrintController {

    private final PrintService service;

    @Autowired
    public PrintController(PrintService service) {
        this.service = service;
    }

    /**
     * AC-12.4.1 模式一 ZPL/TSPL 直连
     */
    @Operation(summary = "模式一 ZPL/TSPL 直连打印（Socket 9100 · 3s 硬性超时）")
    @PostMapping("/labels/zpl")
    public Result<ZplPrintResult> printZpl(
            @RequestBody @Valid ZplPrintRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Name", defaultValue = "操作员") String userName,
            @RequestParam(defaultValue = "1") Long tenantId) {
        return service.printZpl(req, userId, userName, tenantId);
    }

    /**
     * AC-12.4.2 模式二 A4 PDF 浏览器（3×9=27 标签/页 · 30 项/请求）
     */
    @Operation(summary = "模式二 A4 PDF（3×9=27 标签/页 · base64 返回）")
    @PostMapping("/labels/pdf-a4")
    public Result<Map<String, Object>> printPdfA4(
            @RequestBody @Valid PdfA4PrintRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Name", defaultValue = "操作员") String userName,
            @RequestParam(defaultValue = "1") Long tenantId) {
        return service.printPdfA4(req, userId, userName, tenantId);
    }

    /**
     * AC-12.4.4 打印历史查询（分页 + 多维过滤）
     */
    @Operation(summary = "打印历史查询（分页 + 多维过滤）")
    @GetMapping("/logs")
    public Result<PageResponse<PrintLogResponse>> listPrintLogs(
            @RequestParam(required = false) String codeType,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String codeValue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "1") Long tenantId) {
        return service.listPrintLogs(codeType, mode, status, operatorId, codeValue,
                dateFrom == null ? null : dateFrom.atStartOfDay(),
                dateTo == null ? null : dateTo.plusDays(1).atStartOfDay(),
                page, size, tenantId);
    }

    /**
     * AC-12.4.3 单条详情
     */
    @Operation(summary = "单条打印日志详情")
    @GetMapping("/logs/{id}")
    public Result<PrintLogResponse> getPrintLog(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") Long tenantId) {
        return service.getPrintLog(id, tenantId);
    }

    /**
     * AC-12.4.4 补打（同模式/换模式 · 防 reference 递归）
     */
    @Operation(summary = "补打（同模式/换模式）· 防 reference 递归")
    @PostMapping("/logs/{id}/replay")
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    public Result<Map<String, Object>> replayPrintLog(
            @PathVariable Long id,
            @RequestBody(required = false) ReprintRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Name", defaultValue = "操作员") String userName,
            @RequestParam(defaultValue = "1") Long tenantId) {
        if (req == null) req = new ReprintRequest();
        return service.replay(id, req, userId, userName, tenantId);
    }

    /**
     * AC-12.4.4 统计聚合（按月/人/类型/模式）
     */
    @Operation(summary = "打印统计（groupBy 聚合）")
    @GetMapping("/statistics")
    public Result<List<PrintStatisticsBucket>> getStatistics(
            @RequestParam(defaultValue = "month") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "1") Long tenantId) {
        return service.getStatistics(groupBy,
                dateFrom == null ? null : dateFrom.atStartOfDay(),
                dateTo == null ? null : dateTo.plusDays(1).atStartOfDay(),
                tenantId);
    }
}
