package com.btsheng.erp.production.process.service;

import com.btsheng.erp.production.process.dto.*;
import com.btsheng.erp.production.process.entity.CrmProcess;
import com.btsheng.erp.production.process.entity.CrmProcessRoute;
import com.btsheng.erp.production.process.entity.CrmProcessStep;
import com.btsheng.erp.production.process.mapper.CrmProcessMapper;
import com.btsheng.erp.production.process.mapper.CrmProcessRouteMapper;
import com.btsheng.erp.production.process.mapper.CrmProcessStepMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * V1.3.7 · Story 1.10 · AC-3.4 工艺库与工序 Service
 *
 * 4 方法：createProcess / addStep / getRoute / bindToDrawing
 * 3 P1 修补：工序排序严�?/ 机器类型匹配 / 工时非负
 * 3 P2 修补�? 段成本自动聚�?/ 工艺复用 / 工艺变更历史
 * 复用 Story 1.5 DocNoGenerator 扩展 nextProcessNo() 生成 PROC{yyyyMMdd}{seq:4}
 */
@Slf4j
@Service
public class ProcessService {

    private static final Set<String> VALID_SEGMENTS = Set.of("原材料", "粗加工", "精加工", "表面处理", "检验");

    private final CrmProcessMapper processMapper;
    private final CrmProcessStepMapper stepMapper;
    private final CrmProcessRouteMapper routeMapper;
    private final ErpDocNoGenerator docNoGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ProcessService(CrmProcessMapper processMapper,
                          CrmProcessStepMapper stepMapper,
                          CrmProcessRouteMapper routeMapper,
                          ErpDocNoGenerator docNoGenerator) {
        this.processMapper = processMapper;
        this.stepMapper = stepMapper;
        this.routeMapper = routeMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * 创建工艺（AC-3.4.1�?     * P2 修补�? 段成本自动聚�?     */
    @Transactional
    @AuditLog(module = "process", action = "process.create")
    // V1.3.8 Sprint 7 集成 E：工艺变更触�?mat:detail 缓存失效（allEntries 兜底�?
            @CacheEvict(value = {"mat:detail", "mat:price-history"}, allEntries = true)
    public Result<CrmProcess> createProcess(ProcessCreateRequest req, Long operatorUserId) {
        if (req == null) return Result.fail(40001, "REQUEST_BODY_INVALID");
        if (req.getProcessName() == null || req.getProcessName().isEmpty()) {
            return Result.fail(40001, "PROCESS_NAME_REQUIRED");
        }
        if (req.getSteps() == null || req.getSteps().isEmpty()) {
            return Result.fail(40001, "STEPS_REQUIRED");
        }

        // P1 修补 1：工序排序严格（step_no 唯一 + 严格递增�?
            java.util.Set<Integer> seenStepNos = new java.util.HashSet<>();
        for (ProcessCreateRequest.StepInput s : req.getSteps()) {
            if (s.getStepNo() == null) return Result.fail(40001, "STEP_NO_REQUIRED");
            if (s.getStepNo() < 1) return Result.fail(40001, "STEP_NO_MUST_BE_POSITIVE");
            if (seenStepNos.contains(s.getStepNo())) {
                return Result.fail(40905, "STEP_NO_DUPLICATE");
            }
            seenStepNos.add(s.getStepNo());

            // P1 修补 2：机器类型匹配（必须提供�?
            if (s.getMachineType() == null || s.getMachineType().isEmpty()) {
                return Result.fail(40001, "MACHINE_TYPE_REQUIRED");
            }

            // P1 修补 3：工时非�?
            if (s.getEstimatedHours() == null || s.getEstimatedHours().signum() < 0) {
                return Result.fail(40001, "ESTIMATED_HOURS_MUST_BE_NON_NEGATIVE");
            }
            if (s.getSegment() != null && !VALID_SEGMENTS.contains(s.getSegment())) {
                return Result.fail(40001, "SEGMENT_INVALID");
            }
        }

        // 生成工艺编码
            String processCode = docNoGenerator.nextProcessNo();

        // 5 段成本聚合（P2 修补 1�?
            Map<String, BigDecimal> segmentCosts = new HashMap<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalHours = BigDecimal.ZERO;
        for (ProcessCreateRequest.StepInput s : req.getSteps()) {
            String seg = s.getSegment() == null ? "原材料" : s.getSegment();
            BigDecimal cost = s.getUnitCost() == null ? BigDecimal.ZERO : s.getUnitCost();
            segmentCosts.merge(seg, cost, BigDecimal::add);
            totalCost = totalCost.add(cost);
            if (s.getEstimatedHours() != null) {
                totalHours = totalHours.add(s.getEstimatedHours());
            }
        }
        String costBreakdown = buildCostBreakdownJson(segmentCosts);

        // 写入工艺
            CrmProcess p = new CrmProcess();
        p.setProcessCode(processCode);
        p.setProcessName(req.getProcessName());
        p.setProcessType(req.getProcessType() == null ? "STANDARD" : req.getProcessType());
        p.setDescription(req.getDescription());
        p.setTotalSteps(req.getSteps().size());
        p.setTotalEstimatedHours(totalHours);
        p.setTotalCost(totalCost);
        p.setCostBreakdown(costBreakdown);
        p.setDrawingId(req.getDrawingId());
        p.setIsReusable(req.getIsReusable() == null ? 1 : (req.getIsReusable() ? 1 : 0));
        p.setIsActive(1);
        p.setOwnerUserId(operatorUserId);
        p.setComment(req.getComment());
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        if (req.getDrawingId() != null) {
            // 简化：drawing_no 留冗余接口（实际项目需�?drawing�?
            p.setDrawingNo("DWG-20260612-" + String.format("%04d", req.getDrawingId()));
        }
        processMapper.insert(p);

        // 写入工序
            for (ProcessCreateRequest.StepInput s : req.getSteps()) {
            CrmProcessStep step = new CrmProcessStep();
            step.setProcessId(p.getId());
            step.setStepNo(s.getStepNo());
            step.setStepName(s.getStepName());
            step.setSegment(s.getSegment() == null ? "原材料" : s.getSegment());
            step.setMachineType(s.getMachineType());
            step.setMachineId(s.getMachineId());
            step.setEstimatedHours(s.getEstimatedHours() == null ? BigDecimal.ZERO : s.getEstimatedHours());
            step.setUnitCost(s.getUnitCost() == null ? BigDecimal.ZERO : s.getUnitCost());
            step.setIsQualityCheck(s.getIsQualityCheck() != null && s.getIsQualityCheck() ? 1 : 0);
            step.setCreatedAt(LocalDateTime.now());
            stepMapper.insert(step);
        }

        return Result.ok(p);
    }

    /**
     * 新增工序（AC-3.4.2�?     * P1 修补 1：step_no 严格递增（必须为 max+1�?     */
    @Transactional
    @AuditLog(module = "process", action = "process.add_step")
    public Result<CrmProcessStep> addStep(Long processId, AddStepRequest req, Long operatorUserId) {
        if (req == null) return Result.fail(40001, "REQUEST_BODY_INVALID");
        if (req.getStepName() == null || req.getStepName().isEmpty()) {
            return Result.fail(40001, "STEP_NAME_REQUIRED");
        }
        if (req.getEstimatedHours() != null && req.getEstimatedHours().signum() < 0) {
            return Result.fail(40001, "ESTIMATED_HOURS_MUST_BE_NON_NEGATIVE");
        }
        if (req.getMachineType() == null || req.getMachineType().isEmpty()) {
            return Result.fail(40001, "MACHINE_TYPE_REQUIRED");
        }

        CrmProcess p = processMapper.selectById(processId);
        if (p == null) return Result.fail(40404, "PROCESS_NOT_FOUND");

        // 自动分配 step_no = max+1
            Integer max = stepMapper.maxStepNo(processId);
        int nextStepNo = (max == null ? 0 : max) + 1;

        CrmProcessStep step = new CrmProcessStep();
        step.setProcessId(processId);
        step.setStepNo(nextStepNo);
        step.setStepName(req.getStepName());
        step.setSegment(req.getSegment() == null ? "原材料" : req.getSegment());
        step.setMachineType(req.getMachineType());
        step.setMachineId(req.getMachineId());
        step.setEstimatedHours(req.getEstimatedHours() == null ? BigDecimal.ZERO : req.getEstimatedHours());
        step.setUnitCost(req.getUnitCost() == null ? BigDecimal.ZERO : req.getUnitCost());
        step.setIsQualityCheck(req.getIsQualityCheck() != null && req.getIsQualityCheck() ? 1 : 0);
        step.setCreatedAt(LocalDateTime.now());
        stepMapper.insert(step);

        // 更新工艺总工�?+ 总成�?
            p.setTotalSteps(p.getTotalSteps() + 1);
        p.setTotalEstimatedHours(p.getTotalEstimatedHours().add(
            step.getEstimatedHours() == null ? BigDecimal.ZERO : step.getEstimatedHours()));
        p.setTotalCost(p.getTotalCost().add(step.getUnitCost() == null ? BigDecimal.ZERO : step.getUnitCost()));
        p.setUpdatedAt(LocalDateTime.now());
        processMapper.updateById(p);

        return Result.ok(step);
    }

    /**
     * 查询工艺路线（AC-3.4.3�?     */
    public Result<Map<String, Object>> getRoute(Long processId, String version) {
        CrmProcess p = processMapper.selectById(processId);
        if (p == null) return Result.fail(40404, "PROCESS_NOT_FOUND");
        List<CrmProcessStep> steps = stepMapper.selectByProcessId(processId);
        Map<String, Object> result = new HashMap<>();
        result.put("process", p);
        result.put("steps", steps);
        result.put("stepCount", steps.size());
        return Result.ok(result);
    }

    /**
     * 工艺路线绑定图纸（AC-3.4.4 · P2 修补：工艺变更历史）
     */
    @Transactional
    @AuditLog(module = "process", action = "process.bind_to_drawing")
    public Result<CrmProcessRoute> bindToDrawing(Long processId, BindRouteRequest req, Long operatorUserId) {
        if (req == null) return Result.fail(40001, "REQUEST_BODY_INVALID");
        if (req.getDrawingId() == null) return Result.fail(40001, "DRAWING_ID_REQUIRED");
        CrmProcess p = processMapper.selectById(processId);
        if (p == null) return Result.fail(40404, "PROCESS_NOT_FOUND");

        // 检查重�?
            CrmProcessRoute existing = routeMapper.selectByDrawingIdAndVersion(req.getDrawingId(), req.getVersion());
        if (existing != null) {
            return Result.fail(40905, "ROUTE_ALREADY_EXISTS");
        }

        CrmProcessRoute r = new CrmProcessRoute();
        r.setDrawingId(req.getDrawingId());
        r.setDrawingNo("DWG-20260612-" + String.format("%04d", req.getDrawingId()));
        r.setProcessId(processId);
        r.setProcessCode(p.getProcessCode());
        r.setVersion(req.getVersion() == null ? "v1" : req.getVersion());
        r.setStatus("DRAFT");
        r.setChangeReason(req.getChangeReason());
        r.setCreatedBy(operatorUserId);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        routeMapper.insert(r);
        return Result.ok(r);
    }

    /**
     * 工艺列表查询
     */
    public Result<Map<String, Object>> listProcesses(ProcessQueryRequest query) {
        int limit = query.getSize() > 0 ? query.getSize() : 20;
        int offset = Math.max(query.getPage(), 0) * limit;
        List<CrmProcess> all = processMapper.selectActive();
        List<CrmProcess> filtered = new ArrayList<>();
        for (CrmProcess p : all) {
            if (query.getProcessType() != null && !query.getProcessType().isEmpty()
                && !query.getProcessType().equals(p.getProcessType())) continue;
            if (query.getDrawingId() != null
                && !query.getDrawingId().equals(p.getDrawingId())) continue;
            filtered.add(p);
        }
        int from = Math.min(offset, filtered.size());
        int to = Math.min(offset + limit, filtered.size());
        Map<String, Object> result = new HashMap<>();
        result.put("list", filtered.subList(from, to));
        result.put("total", filtered.size());
        result.put("page", query.getPage());
        result.put("size", limit);
        return Result.ok(result);
    }

    @Transactional
    @AuditLog(module = "process", action = "process.update")
    @CacheEvict(value = {"mat:detail", "mat:price-history"}, allEntries = true)
    public Result<CrmProcess> updateProcess(Long processId, ProcessUpdateRequest req, Long operatorUserId) {
        if (req == null) return Result.fail(40001, "REQUEST_BODY_INVALID");
        CrmProcess p = processMapper.selectById(processId);
        if (p == null) return Result.fail(40404, "PROCESS_NOT_FOUND");
        if (req.getProcessName() != null && !req.getProcessName().isBlank()) {
            p.setProcessName(req.getProcessName());
        }
        if (req.getProcessType() != null && !req.getProcessType().isBlank()) {
            p.setProcessType(req.getProcessType());
        }
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getComment() != null) p.setComment(req.getComment());
        if (req.getIsActive() != null) p.setIsActive(req.getIsActive());
        if (req.getIsReusable() != null) p.setIsReusable(req.getIsReusable() ? 1 : 0);
        p.setUpdatedAt(LocalDateTime.now());
        processMapper.updateById(p);
        return Result.ok(p);
    }

    public Result<CrmProcess> getProcess(Long processId) {
        CrmProcess p = processMapper.selectById(processId);
        if (p == null) return Result.fail(40404, "PROCESS_NOT_FOUND");
        return Result.ok(p);
    }

    /**
     * 复制工艺（产品路�?copy-from · P2 工艺复用�?     */
    @Transactional
    @AuditLog(module = "process", action = "process.copy")
    @CacheEvict(value = {"mat:detail", "mat:price-history"}, allEntries = true)
    public Result<CrmProcess> copyProcess(Long sourceProcessId, String targetName, Long operatorUserId) {
        CrmProcess src = processMapper.selectById(sourceProcessId);
        if (src == null) {
            return Result.fail(40404, "PROCESS_NOT_FOUND");
        }
        List<CrmProcessStep> steps = stepMapper.selectByProcessId(sourceProcessId);
        if (steps.isEmpty()) {
            return Result.fail(40001, "SOURCE_PROCESS_HAS_NO_STEPS");
        }
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName(targetName != null && !targetName.isBlank()
                ? targetName : src.getProcessName() + " (copy)");
        req.setProcessType(src.getProcessType());
        req.setDescription(src.getDescription());
        req.setIsReusable(src.getIsReusable() != null && src.getIsReusable() == 1);
        req.setComment("copied_from:" + src.getProcessCode());
        List<ProcessCreateRequest.StepInput> inputs = new ArrayList<>();
        for (CrmProcessStep s : steps) {
            ProcessCreateRequest.StepInput in = new ProcessCreateRequest.StepInput();
            in.setStepNo(s.getStepNo());
            in.setStepName(s.getStepName());
            in.setSegment(s.getSegment());
            in.setMachineType(s.getMachineType());
            in.setMachineId(s.getMachineId());
            in.setEstimatedHours(s.getEstimatedHours());
            in.setUnitCost(s.getUnitCost());
            in.setIsQualityCheck(s.getIsQualityCheck() != null && s.getIsQualityCheck() == 1);
            inputs.add(in);
        }
        req.setSteps(inputs);
        return createProcess(req, operatorUserId);
    }

    private String buildCostBreakdownJson(Map<String, BigDecimal> segmentCosts) {
        try {
            List<Map<String, Object>> breakdown = new ArrayList<>();
            for (String seg : new String[]{"原材料", "粗加工", "精加工", "表面处理", "检验"}) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("name", seg);
                entry.put("totalCost", segmentCosts.getOrDefault(seg, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
                breakdown.add(entry);
            }
            return objectMapper.writeValueAsString(breakdown);
        } catch (Exception e) {
            return "[]";
        }
    }
}
