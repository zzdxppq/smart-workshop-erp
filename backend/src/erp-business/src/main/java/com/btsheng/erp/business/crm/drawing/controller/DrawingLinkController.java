package com.btsheng.erp.business.crm.drawing.controller;

import com.btsheng.erp.business.crm.drawing.dto.AccessibleDrawingListResponse;
import com.btsheng.erp.business.crm.drawing.dto.DrawingLinkListResponse;
import com.btsheng.erp.business.crm.drawing.dto.OperatorProcessDrawingResponse;
import com.btsheng.erp.business.crm.drawing.service.DrawingLinkQueryService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * V1.3.9 Sprint 13 Story 13.3 · 图纸-业务单据真实查询控制器（3 端点）
 *
 * <ul>
 *   <li>GET /drawings/{id}/links?biz_type=ORDER · 端点 1 · 图纸 → 关联 bizIds（无缓存）</li>
 *   <li>GET /drawings/accessible?biz_type=ORDER&biz_id=123 · 端点 2 · 业务单据 → 可访问图纸（Redis 5min）</li>
 *   <li>GET /drawings/process/{processId} · 端点 3 · OPERATOR 工序 → 图纸（Redis 5min）</li>
 * </ul>
 *
 * <p>错误码：
 * <ul>
 *   <li>40001 INVALID_BIZ_TYPE · biz_type 不在 5 类枚举内</li>
 *   <li>40304 DRAWING_FORBIDDEN / PROCESS_FORBIDDEN · 角色与 biz_type 不匹配 / 工序非本人操作</li>
 *   <li>40401 DRAWING_NOT_FOUND / BIZ_DOC_NOT_FOUND</li>
 *   <li>40402 PROCESS_NOT_FOUND</li>
 * </ul>
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/drawings")
@Tag(name = "E3-Drawing-Link-Query", description = "Epic 3 图纸-业务单据真实查询（Story 13.3）")
public class DrawingLinkController {

    private final DrawingLinkQueryService linkQueryService;

    @Autowired
    public DrawingLinkController(DrawingLinkQueryService linkQueryService) {
        this.linkQueryService = linkQueryService;
    }

    /**
     * 端点 1 · 图纸 → 关联 bizIds（按 biz_type 单类过滤）
     *
     * <p>5 类 link JOIN SQL 真实查询 · 无缓存（高频但非热点）
     */
    @GetMapping("/{id}/links")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "图纸 → 关联业务单据 ID 列表（13.3 端点 1 · 按 biz_type 过滤）")
    public Result<DrawingLinkListResponse> getLinks(
            @PathVariable("id") @Parameter(description = "图纸 ID") Long id,
            @RequestParam("biz_type") @Parameter(description = "业务类型",
                    example = "ORDER") String bizType,
            Authentication auth) {
        return linkQueryService.getLinksByDrawing(id, bizType, auth);
    }

    /**
     * 端点 2 · 业务单据 → 可访问图纸列表（Redis 5min 缓存）
     *
     * <p>12.1 web-impl `<DrawingViewer>` 真实数据接入入口 · 替代 mock [10, 20, 30]
     */
    @GetMapping("/accessible")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "业务单据 → 可访问图纸列表（13.3 端点 2 · Redis 5min 缓存）")
    public Result<AccessibleDrawingListResponse> getAccessible(
            @RequestParam("biz_type") @Parameter(description = "业务类型",
                    example = "ORDER") String bizType,
            @RequestParam("biz_id") @Parameter(description = "业务单据 ID",
                    example = "123") Long bizId,
            Authentication auth) {
        return linkQueryService.getAccessibleDrawings(bizType, bizId, auth);
    }

    /**
     * 端点 3 · OPERATOR 工序扫码 → 可访问图纸列表（Redis 5min 缓存 · 手机端高频）
     */
    @GetMapping("/process/{processId}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','OPERATOR')")
    @Operation(summary = "OPERATOR 工序 → 可访问图纸列表（13.3 端点 3 · Redis 5min 缓存）")
    public Result<OperatorProcessDrawingResponse> getProcessDrawings(
            @PathVariable("processId") @Parameter(description = "工序 ID",
                    example = "500") Long processId,
            Authentication auth) {
        return linkQueryService.getOperatorProcessDrawings(processId, auth);
    }
}
