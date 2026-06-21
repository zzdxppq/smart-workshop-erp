package com.btsheng.erp.business.crm.qualityfa.controller;

import com.btsheng.erp.business.crm.qualityfa.dto.FaCreateRequest;
import com.btsheng.erp.business.crm.qualityfa.entity.CrmQualityFa;
import com.btsheng.erp.business.crm.qualityfa.service.QualityFaService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.29 · 品质·FA 首件 Controller (FR-7-2)
 *
 * <p>4 端点：
 * <ul>
 *   <li>POST /quality-fa              创建 FA 首件（AC-7.2.1 · 开工前必检）</li>
 *   <li>POST /quality-fa/{id}/pass     PASSED（AC-7.2.2 · PDF 报告）</li>
 *   <li>POST /quality-fa/{id}/reject   FAILED（AC-7.2.3 · 锁定工序）</li>
 *   <li>GET  /quality-fa              列表</li>
 * </ul>
 *
 * <p>V2.1 品质专项增强（双签流程）：
 * <ul>
 *   <li>POST /quality-fa/{id}/inspector-sign  品检员签字</li>
 *   <li>POST /quality-fa/{id}/engineer-sign   工程师终签</li>
 *   <li>POST /quality-fa/{id}/rework          驳回返工</li>
 *   <li>POST /quality-fa/{id}/resubmit        重新提交检验</li>
 * </ul>
 */
@RestController
@RequestMapping("/quality-fa")
@Tag(name = "E7-Quality-FA", description = "FA 首件（Story 1.29 FR-7-2）")
public class QualityFaController {

    private final QualityFaService service;

    @Autowired
    public QualityFaController(QualityFaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建 FA 首件（AC-7.2.1 · 开工前必检 · 8 维度）")
    public Result<CrmQualityFa> create(
            @RequestBody FaCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createFa(req, userId);
    }

    @PostMapping("/{id}/pass")
    @Operation(summary = "标记 PASSED（AC-7.2.2 · PDF 报告）")
    public Result<CrmQualityFa> pass(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.pass(id, userId);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "标记 FAILED（AC-7.2.3 · 锁定工序阻断生产）")
    public Result<Map<String, Object>> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.reject(id, reason, userId);
    }

    @GetMapping
    @Operation(summary = "查询 FA 首件列表")
    public Result<List<CrmQualityFa>> list(
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) Long processId,
            @RequestParam(required = false) String result) {
        return service.list(workOrderId, processId, result);
    }

    @PostMapping("/{id}/inspector-sign")
    @Operation(summary = "V2.1 品检员签字（双签第一步）")
    public Result<CrmQualityFa> inspectorSign(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.inspectorSign(id, userId);
    }

    @PostMapping("/{id}/engineer-sign")
    @Operation(summary = "V2.1 工程师终签（双签第二步）")
    public Result<CrmQualityFa> engineerSign(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.engineerSign(id, userId);
    }

    @PostMapping("/{id}/rework")
    @Operation(summary = "V2.1 驳回并要求返工")
    public Result<CrmQualityFa> rework(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.rework(id, reason, userId);
    }

    @PostMapping("/{id}/resubmit")
    @Operation(summary = "V2.1 重新提交检验（返工后）")
    public Result<CrmQualityFa> resubmit(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.resubmit(id, userId);
    }
}
