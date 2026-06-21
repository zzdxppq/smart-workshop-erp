package com.btsheng.erp.platform.label.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.label.dto.LabelPreviewRequest;
import com.btsheng.erp.platform.label.dto.LabelPreviewResponse;
import com.btsheng.erp.platform.label.dto.UpdateLabelTemplateRequest;
import com.btsheng.erp.platform.label.service.LabelTemplateService;
import com.btsheng.erp.core.web.PreAuthorizeRoles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 标签模板 Controller（V1.3.9 Sprint 12 · Story 12.3 · 2 端点）
 *
 * <p>命名空间对齐 12.2 /printers、12.4 /print/labels（architect REVIEW §1.1）
 * <p>2 端点：listTemplates + preview · 任意登录用户可访问（与 /printers/available 一致）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Tag(name = "E12-Label", description = "标签模板 4 种 · 列表 + 预览渲染 base64 PNG")
@RestController
@RequestMapping("/label-templates")
public class LabelTemplateController {

    private final LabelTemplateService labelTemplateService;

    @Autowired
    public LabelTemplateController(LabelTemplateService labelTemplateService) {
        this.labelTemplateService = labelTemplateService;
    }

    /**
     * TC-12.3.2.1 / 2.2 — 查标签模板元数据
     *
     * <p>type 为空时返回 5 种（GD/LZ/SB/WW/WL · SB 由代码层 fallback 注入）
     * <p>type 不为空时返回单元素或 40002（type 不支持）
     */
    @Operation(summary = "查标签模板元数据（type 可选 · 不传返回 5 种）")
    @GetMapping
    public Result<Map<String, Object>> listTemplates(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Long tenantId) {
        return labelTemplateService.listTemplates(type, tenantId);
    }

    /**
     * TC-12.3.2.3 / 2.4 — 生成标签预览 PNG base64
     *
     * <p>入参：type (GD/LZ/SB/WW/WL) + data {qr_content, lines, factory_name} + format (默认 PNG)
     * <p>输出：base64 PNG (data:image/png;base64,...)
     */
    @Operation(summary = "生成标签预览 PNG base64（ZXing + 三区布局）")
    @PostMapping("/preview")
    public Result<LabelPreviewResponse> preview(
            @Valid @RequestBody LabelPreviewRequest request,
            @RequestParam(defaultValue = "1") Long tenantId) {
        return labelTemplateService.preview(request, tenantId);
    }

    @PutMapping("/{type}")
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    @Operation(summary = "更新标签模板（管理员 · GD/LZ/WW/WL）")
    public Result<?> updateTemplate(
            @PathVariable String type,
            @RequestBody UpdateLabelTemplateRequest request,
            @RequestParam(defaultValue = "1") Long tenantId) {
        return labelTemplateService.updateTemplate(type, request, tenantId);
    }
}