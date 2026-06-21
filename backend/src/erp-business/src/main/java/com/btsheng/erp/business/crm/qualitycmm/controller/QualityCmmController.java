package com.btsheng.erp.business.crm.qualitycmm.controller;

import com.btsheng.erp.business.crm.qualitycmm.dto.AddCmmPointRequest;
import com.btsheng.erp.business.crm.qualitycmm.dto.CmmCreateRequest;
import com.btsheng.erp.business.crm.qualitycmm.entity.CrmQualityCmm;
import com.btsheng.erp.business.crm.qualitycmm.entity.CrmQualityCmmPoint;
import com.btsheng.erp.business.crm.qualitycmm.service.QualityCmmService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.30 · 品质·CMM 三次元 Controller (FR-7-3)
 *
 * <p>4 端点：
 * <ul>
 *   <li>POST /quality-cmm                  创建 CMM 单（AC-7.3.1）</li>
 *   <li>POST /quality-cmm/{id}/point        追加测点</li>
 *   <li>GET  /quality-cmm/{id}/report       CPK 报告（AC-7.3.2/7.3.3）</li>
 *   <li>GET  /quality-cmm                  列表</li>
 * </ul>
 */
@RestController
@RequestMapping("/quality-cmm")
@Tag(name = "E7-Quality-CMM", description = "CMM 三次元（Story 1.30 FR-7-3）")
public class QualityCmmController {

    private final QualityCmmService service;

    @Autowired
    public QualityCmmController(QualityCmmService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建 CMM 单（AC-7.3.1 · 测点 ≥ 3）")
    public Result<CrmQualityCmm> create(
            @RequestBody CmmCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createCmm(req, userId);
    }

    @PostMapping("/{id}/point")
    @Operation(summary = "追加 CMM 测点")
    public Result<CrmQualityCmmPoint> addPoint(
            @RequestBody AddCmmPointRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.addPoint(req, userId);
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "获取 CPK 报告（AC-7.3.2/7.3.3 · 偏差超差告警）")
    public Result<Map<String, Object>> getReport(@PathVariable Long id) {
        return service.getReport(id);
    }

    @GetMapping
    @Operation(summary = "查询 CMM 列表")
    public Result<List<CrmQualityCmm>> list(
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) String result) {
        return service.listCmms(workOrderId, result);
    }
}
