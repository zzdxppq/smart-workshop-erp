package com.btsheng.erp.business.crm.qualityinspection.controller;

import com.btsheng.erp.business.crm.qualityinspection.dto.AddInspectionItemRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionCreateRequest;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspection;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspectionItem;
import com.btsheng.erp.business.crm.qualityinspection.service.QualityInspectionService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.28 · 品质·来料/过程/成品检 Controller (FR-7-1)
 *
 * <p>5 端点：
 * <ul>
 *   <li>POST /quality-inspection                 创建检单（AC-7.1.1/7.1.2/7.1.3）</li>
 *   <li>POST /quality-inspection/{id}/item       追加检验项目（P1 修补 2）</li>
 *   <li>POST /quality-inspection/{id}/pass       PASSED（OQC 触发入库）</li>
 *   <li>POST /quality-inspection/{id}/reject     FAILED（IQC 触发返修）</li>
 *   <li>GET  /quality-inspection                 列表</li>
 * </ul>
 */
@RestController
@RequestMapping("/quality-inspection")
@Tag(name = "E7-Quality-Inspection", description = "来料/过程/成品检（Story 1.28 FR-7-1）")
public class QualityInspectionController {

    private final QualityInspectionService service;

    @Autowired
    public QualityInspectionController(QualityInspectionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建检单 IQC/IPQC/OQC（AC-7.1.1/7.1.2/7.1.3）")
    public Result<CrmQualityInspection> create(
            @RequestBody InspectionCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createInspection(req, userId);
    }

    @PostMapping("/{id}/item")
    @Operation(summary = "追加检验项目（P1 修补 2 · 严重度 4 级）")
    public Result<CrmQualityInspectionItem> addItem(
            @RequestBody AddInspectionItemRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.addItem(req, userId);
    }

    @PostMapping("/{id}/pass")
    @Operation(summary = "标记 PASSED（OQC 触发入库 · AC-7.1.4）")
    public Result<Map<String, Object>> pass(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.pass(id, userId);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "标记 FAILED（IQC 触发返修 · AC-7.1.4）")
    public Result<Map<String, Object>> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.reject(id, reason, userId);
    }

    @GetMapping
    @Operation(summary = "查询检单列表")
    public Result<List<CrmQualityInspection>> list(
            @RequestParam(required = false) String inspectType,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) String result) {
        return service.list(inspectType, materialId, workOrderId, result);
    }
}
