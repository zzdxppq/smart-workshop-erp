package com.btsheng.erp.business.crm.qualitydefect.controller;

import com.btsheng.erp.business.crm.qualitydefect.dto.AddDefectActionRequest;
import com.btsheng.erp.business.crm.qualitydefect.dto.DefectCreateRequest;
import com.btsheng.erp.business.crm.qualitydefect.entity.CrmQualityDefect;
import com.btsheng.erp.business.crm.qualitydefect.entity.CrmQualityDefectAction;
import com.btsheng.erp.business.crm.qualitydefect.service.QualityDefectService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.31 · 品质·不良品处理 Controller (FR-7-4)
 *
 * <p>4 端点：
 * <ul>
 *   <li>POST /quality-defect              不良品登记（AC-7.4.1 · 8D）</li>
 *   <li>POST /quality-defect/{id}/action   添加处理动作（AC-7.4.2 · 3 动作）</li>
 *   <li>POST /quality-defect/{id}/resolve  解决关闭（AC-7.4.3 · PPM）</li>
 *   <li>GET  /quality-defect              列表</li>
 * </ul>
 *
 * <p>V2.1 品质专项增强：
 * <ul>
 *   <li>POST /quality-defect/{id}/create-rework-wo   返工自动创建工单</li>
 *   <li>POST /quality-defect/{id}/scrap-inventory    报废扣减库存</li>
 *   <li>POST /quality-defect/{id}/concession-approve 让步接收审批</li>
 * </ul>
 */
@RestController
@RequestMapping("/quality-defect")
@Tag(name = "E7-Quality-Defect", description = "不良品处理（Story 1.31 FR-7-4）")
public class QualityDefectController {

    private final QualityDefectService service;

    @Autowired
    public QualityDefectController(QualityDefectService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "不良品登记（AC-7.4.1 · 8D 报告）")
    public Result<CrmQualityDefect> create(
            @RequestBody DefectCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createDefect(req, userId);
    }

    @PostMapping("/{id}/action")
    @Operation(summary = "添加处理动作（AC-7.4.2 · 返工/报废/让步接收）")
    public Result<CrmQualityDefectAction> addAction(
            @RequestBody AddDefectActionRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.addAction(req, userId);
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "解决并关闭（AC-7.4.3 · PPM 不良率）")
    public Result<Map<String, Object>> resolve(
            @PathVariable Long id,
            @RequestParam(required = false) String closure,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.resolve(id, closure, userId);
    }

    @GetMapping
    @Operation(summary = "查询不良品单列表")
    public Result<List<CrmQualityDefect>> list(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String result) {
        return service.list(sourceType, status, result);
    }

    @PostMapping("/{id}/create-rework-wo")
    @Operation(summary = "V2.1 返工自动创建工单")
    public Result<Map<String, Object>> createReworkWorkOrder(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createReworkWorkOrder(id, userId);
    }

    @PostMapping("/{id}/scrap-inventory")
    @Operation(summary = "V2.1 报废扣减库存")
    public Result<Map<String, Object>> scrapInventory(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.scrapInventory(id, userId);
    }

    @PostMapping("/{id}/concession-approve")
    @Operation(summary = "V2.1 让步接收审批")
    public Result<CrmQualityDefect> concessionApprove(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean approved,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.concessionApprove(id, approved, userId);
    }
}
