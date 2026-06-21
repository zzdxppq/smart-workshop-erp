package com.btsheng.erp.business.crm.qualityinspection.controller;

import com.btsheng.erp.business.crm.qualityinspection.dto.ConcessionApproveRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionV1389CreateRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionV1389Response;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionV1389SubmitRequest;
import com.btsheng.erp.business.crm.qualityinspection.service.QualityInspectionV1389Service;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.9 Sprint 13.1 · POST/GET /quality/inspections（OpenAPI 契约）
 */
@RestController
@RequestMapping("/quality/inspections")
@Tag(name = "E7-Quality-V1389", description = "品质检验 V1.3.9 OpenAPI 契约")
public class QualityInspectionV1389Controller {

    private final QualityInspectionV1389Service service;

    @Autowired
    public QualityInspectionV1389Controller(QualityInspectionV1389Service service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建品质检验单（V1.3.9 Sprint 13.1）")
    public InspectionV1389Response create(
            @RequestBody InspectionV1389CreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        Result<InspectionV1389Response> r = service.create(req, userId);
        if (r.getCode() != 0) {
            throw new QualityInspectionV1389Exception(r.getCode(), r.getMessage());
        }
        return r.getData();
    }

    @GetMapping
    @Operation(summary = "检验单列表（分页 + 类型/关键字/状态过滤）")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.list(type, keyword, status, source, pageNum, pageSize);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交检验结果（手机端主力 · PC 亦可提交）")
    public Result<Map<String, Object>> submit(
            @PathVariable Long id,
            @RequestBody InspectionV1389SubmitRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        Result<Map<String, Object>> r = service.submit(id, req, userId);
        if (r.getCode() != 0) {
            throw new QualityInspectionV1389Exception(r.getCode(), r.getMessage());
        }
        return r;
    }

    @GetMapping("/{id}")
    @Operation(summary = "检验单详情")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return service.getDetail(id);
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "检验报告")
    public Result<Map<String, Object>> report(@PathVariable Long id) {
        return service.getReport(id);
    }

    @PostMapping("/{id}/approve-concession")
    @Operation(summary = "让步接收审批（品质主管/生管双签）")
    public Result<Map<String, Object>> approveConcession(
            @PathVariable Long id,
            @RequestBody ConcessionApproveRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        Result<Map<String, Object>> r = service.approveConcession(
                id, req.getApproverRole(), req.getAction(), req.getComment(), userId);
        if (r.getCode() != 0) {
            throw new QualityInspectionV1389Exception(r.getCode(), r.getMessage());
        }
        return r;
    }

    @GetMapping("/{id}/concession-approvals")
    @Operation(summary = "让步接收审批任务列表")
    public Result<List<Map<String, Object>>> concessionApprovals(@PathVariable Long id) {
        return service.getConcessionApprovals(id);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QualityInspectionV1389Exception.class)
    public Result<Void> handle(QualityInspectionV1389Exception ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    static class QualityInspectionV1389Exception extends RuntimeException {
        private final int code;
        QualityInspectionV1389Exception(int code, String message) {
            super(message);
            this.code = code;
        }
        int getCode() { return code; }
    }
}
