package com.btsheng.erp.business.crm.drawing.controller;

import com.btsheng.erp.business.crm.drawing.dto.DrawingPermissionDTO;
import com.btsheng.erp.business.crm.drawing.service.DrawingPreviewService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * V1.3.9 Sprint 12 Story 12.1 · 图纸 ACL 控制器（3 端点）
 *
 * <ul>
 *   <li>GET /drawings/{id}/permission · 任意角色可调（FINANCE 也返 200 + 全 false）</li>
 *   <li>GET /drawings/{id}/preview · @PreAuthorize @drawingAuthz.canView</li>
 *   <li>GET /drawings/{id}/download · @PreAuthorize @drawingAuthz.canDownload</li>
 * </ul>
 *
 * <p>错误码统一 40304 · 与 1.40 工序分配同码 · 简化错误码空间（评审建议）
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/drawings")
@Tag(name = "E3-Drawing-ACL", description = "Epic 3 图纸权限矩阵（Story 12.1）")
public class DrawingAclController {

    private final DrawingPreviewService previewService;

    @Autowired
    public DrawingAclController(DrawingPreviewService previewService) {
        this.previewService = previewService;
    }

    /**
     * 1) 权限查询端点（任意角色 · 返回自身权限位）
     *
     * <p>FINANCE 也返回 200 + 全 false + scope=NONE（不返 403）
     */
    @GetMapping("/{id}/permission")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "查询当前用户对图纸的权限位（任意角色可调）")
    public Result<DrawingPermissionDTO> getPermission(@PathVariable("id") Long id,
                                                       Authentication auth) {
        return previewService.getPermission(id, auth);
    }

    /**
     * 2) 图纸预览（带 ACL · 自动审计）
     */
    @GetMapping(value = "/{id}/preview", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@drawingAuthz.canView(authentication, #id)")
    @Operation(summary = "图纸预览（带 ACL · 自动审计）")
    public ResponseEntity<byte[]> preview(@PathVariable("id") Long id,
                                           @RequestParam(defaultValue = "MEDIUM") String resolution,
                                           Authentication auth) {
        Result<byte[]> result = previewService.previewPDF(id, resolution, auth);
        if (result.getCode() != 0) {
            return ResponseEntity.status(403)
                    .body(("{\"code\":" + result.getCode()
                            + ",\"message\":\"" + result.getMessage() + "\"}").getBytes());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline",
                "drawing-" + id + "-" + System.currentTimeMillis() + ".pdf");
        return ResponseEntity.ok().headers(headers).body(result.getData());
    }

    /**
     * 3) 下载原文件（仅 ENGINEER · 强隔离）
     */
    @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("@drawingAuthz.canDownload(authentication, #id)")
    @Operation(summary = "下载图纸原文件（仅 ENGINEER · 强隔离）")
    public ResponseEntity<byte[]> download(@PathVariable("id") Long id,
                                            Authentication auth) {
        Result<byte[]> result = previewService.downloadOriginal(id, auth);
        if (result.getCode() != 0) {
            return ResponseEntity.status(403)
                    .body(("{\"code\":" + result.getCode()
                            + ",\"message\":\"" + result.getMessage() + "\"}").getBytes());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                "drawing-" + id + "-" + System.currentTimeMillis() + ".pdf");
        return ResponseEntity.ok().headers(headers).body(result.getData());
    }
}