package com.btsheng.erp.business.crm.qualityinspection.controller;

import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionTemplateResponse;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionTemplateSaveRequest;
import com.btsheng.erp.business.crm.qualityinspection.service.QualityInspectionTemplateService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * V1.3.9 · 检验方案模板 CRUD + 发布/停用
 */
@RestController
@RequestMapping("/quality/inspection-templates")
@Tag(name = "E7-Quality-Template", description = "检验方案模板（工程师定义·主管发布）")
public class QualityInspectionTemplateController {

    private final QualityInspectionTemplateService service;

    @Autowired
    public QualityInspectionTemplateController(QualityInspectionTemplateService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "模板列表；传 drawingNo 时返回 ACTIVE 匹配项（含检验项）")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String drawingNo,
            @RequestParam(required = false) String inspectionType,
            @RequestParam(required = false) String materialCode) {
        return service.list(status, drawingNo, inspectionType, materialCode);
    }

    @GetMapping("/{id}")
    @Operation(summary = "模板详情（含检验项）")
    public Result<InspectionTemplateResponse> get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新建草稿模板（工程师/品质主管）")
    public Result<InspectionTemplateResponse> create(
            @RequestBody InspectionTemplateSaveRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        return service.create(req, userId, userRoles);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新草稿模板")
    public Result<InspectionTemplateResponse> update(
            @PathVariable Long id,
            @RequestBody InspectionTemplateSaveRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        return service.update(id, req, userId, userRoles);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除草稿模板")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        return service.delete(id, userRoles);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布模板（品质主管）· 同范围旧 ACTIVE 自动归档")
    public Result<InspectionTemplateResponse> publish(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        return service.publish(id, userId, userRoles);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "停用模板（品质主管）")
    public Result<InspectionTemplateResponse> archive(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        return service.archive(id, userId, userRoles);
    }
}
