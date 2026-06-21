package com.btsheng.erp.production.outsource.eta.controller;

import com.btsheng.erp.production.outsource.eta.dto.PredictEtaRequest;
import com.btsheng.erp.production.outsource.eta.dto.UpdateActualEtaRequest;
import com.btsheng.erp.production.outsource.eta.entity.CrmOutsourceEta;
import com.btsheng.erp.production.outsource.eta.service.OutsourceEtaService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * V1.3.7 · Story 1.24 · 委外历史交期预估 Controller (FR-6-4)
 *
 * <p>3 端点：
 * <ul>
 *   <li>POST /outsource-eta/predict       基于历史数据预估（AC-6.4.1）</li>
 *   <li>POST /outsource-eta/actual       更新实际交期（AC-6.4.2）</li>
 *   <li>GET  /outsource-eta/{outsourceId}/history 预估历史（AC-6.4.3）</li>
 * </ul>
 */
@RestController
@RequestMapping("/outsource-eta")
@Tag(name = "E6-Outsource-Eta", description = "委外历史交期预估（Story 1.24 FR-6-4）")
public class OutsourceEtaController {

    private final OutsourceEtaService service;

    @Autowired
    public OutsourceEtaController(OutsourceEtaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "委外 ETA 预估列表")
    public Result<com.btsheng.erp.core.model.PageResponse<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.listEtas(keyword, pageNum, pageSize);
    }

    @GetMapping("/history-records")
    @Operation(summary = "委外 ETA 历史记录列表")
    public Result<com.btsheng.erp.core.model.PageResponse<Map<String, Object>>> historyRecords(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.listEtaHistoryRecords(keyword, pageNum, pageSize);
    }

    @PostMapping("/predict")
    @Operation(summary = "基于历史数据预估交期（AC-6.4.1）")
    public Result<CrmOutsourceEta> predict(
            @RequestBody PredictEtaRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.predictEta(req, userId);
    }

    @PostMapping("/actual")
    @Operation(summary = "更新实际交期（AC-6.4.2 · 偏差 > 20% 自动告警）")
    public Result<CrmOutsourceEta> actual(
            @RequestBody UpdateActualEtaRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.updateActualEta(req.getEtaId(), req.getActualDeliveryDate(), userId);
    }

    @GetMapping("/{outsourceId}/history")
    @Operation(summary = "预估历史（AC-6.4.3 · 含准确率统计）")
    public Result<Map<String, Object>> history(@PathVariable Long outsourceId) {
        return service.getEtaHistory(outsourceId);
    }
}
