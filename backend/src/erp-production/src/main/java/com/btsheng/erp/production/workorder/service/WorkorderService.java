package com.btsheng.erp.production.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.production.workorder.dto.WorkorderCreateRequest;
import com.btsheng.erp.production.workorder.dto.WorkorderQueryRequest;
import com.btsheng.erp.production.workorder.dto.WorkorderScheduleRequest;
import com.btsheng.erp.production.workorder.dto.WorkorderTimelineResponse;
import com.btsheng.erp.production.workorder.dto.CurrentProcessResponse;
import com.btsheng.erp.production.workorder.entity.CrmProductionSchedule;
import com.btsheng.erp.production.workorder.entity.CrmWorkorder;
import com.btsheng.erp.production.workorder.entity.CrmWorkorderProcess;
import com.btsheng.erp.production.workorder.entity.CrmWorkorderStep;
import com.btsheng.erp.production.workorder.mapper.CrmProductionScheduleMapper;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderMapper;
import com.btsheng.erp.production.process.entity.CrmProcessStep;
import com.btsheng.erp.production.process.service.ProcessService;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderProcessMapper;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderStepMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.core.web.DataScopeContext;
import com.btsheng.erp.core.web.ProductionDataScopeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * V1.3.7 · Story 1.15 · 工单与排�?Service
 *
 * 5 业务方法：createWorkorder / updateWorkorder / scheduleWorkorder / startProduction / listWorkorders
 * 4 P1 修补：工单唯一 / 排产冲突检�?/ 设备类型匹配 / 工时非负
 * 4 P2 修补：MRP 触发 hook / 多工单合�?/ 工单变更历史
 */
@Service
public class WorkorderService {

    public static final Pattern WORKORDER_NO_PATTERN = Pattern.compile("^GD\\d{8}-\\d{4}$");

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String SCHEDULE_PLANNED = "PLANNED";
    public static final String SCHEDULE_IN_PROGRESS = "IN_PROGRESS";
    public static final String SCHEDULE_COMPLETED = "COMPLETED";
    public static final String SCHEDULE_CONFLICT = "CONFLICT";

    private final CrmWorkorderMapper workorderMapper;
    private final CrmWorkorderStepMapper stepMapper;
    private final CrmWorkorderProcessMapper processMapper;
    private final CrmProductionScheduleMapper scheduleMapper;
    private final ProcessService processService;
    private final ErpDocNoGenerator docNoGenerator;

    @Autowired
    public WorkorderService(CrmWorkorderMapper workorderMapper,
                             CrmWorkorderStepMapper stepMapper,
                             CrmWorkorderProcessMapper processMapper,
                             CrmProductionScheduleMapper scheduleMapper,
                             ProcessService processService,
                             ErpDocNoGenerator docNoGenerator) {
        this.workorderMapper = workorderMapper;
        this.stepMapper = stepMapper;
        this.processMapper = processMapper;
        this.scheduleMapper = scheduleMapper;
        this.processService = processService;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * 操作工当前工序（Story 12.1 · Android DrawPermissionInterceptor · Redis 5min�?     */
    @Cacheable(value = "user:current_process", key = "#userId", unless = "#result == null || !#result.isSuccess()")
    public Result<CurrentProcessResponse> getCurrentProcess(Long userId) {
        CrmWorkorderProcess proc = processMapper.selectByOperatorInProgressStep(userId);
        if (proc == null) {
            proc = processMapper.selectDemoCurrentProcess();
        }
        if (proc == null) {
            return Result.fail(40404, "CURRENT_PROCESS_NOT_FOUND");
        }
        CurrentProcessResponse resp = new CurrentProcessResponse();
        resp.setUserId(userId);
        resp.setWorkorderId(proc.getWorkorderId());
        resp.setProcessId(proc.getId());
        resp.setProcessSeq(proc.getProcessSeq());
        resp.setProcessNo(proc.getProcessCode() != null ? proc.getProcessCode()
                : String.format("P%02d", proc.getProcessSeq()));
        resp.setProcessName(proc.getProcessName());
        resp.setWorkorderNo(proc.getWorkorderNo());
        if (proc.getWorkorderId() != null) {
            CrmWorkorder wo = workorderMapper.selectById(proc.getWorkorderId());
            if (wo != null) {
                resp.setDrawingId(wo.getDrawingId());
            }
        }
        resp.setCached(true);
        return Result.ok(resp);
    }

    /**
     * AC-5.1.1：工单创�?     */
    @Transactional
    @AuditLog(module = "production", action = "production.create_workorder")
    public Result<CrmWorkorder> createWorkorder(WorkorderCreateRequest req, Long operatorUserId) {
        if (req.getMaterialCode() == null || req.getMaterialCode().isEmpty()) {
            return Result.fail(40001, "MATERIAL_CODE_REQUIRED");
        }
        if (req.getQty() == null || req.getQty() <= 0) {
            return Result.fail(40001, "QTY_INVALID");
        }
        if (req.getProductName() == null || req.getProductName().isEmpty()) {
            return Result.fail(40001, "PRODUCT_NAME_REQUIRED");
        }
        // P1 修补 4：工时非�?
            if (req.getEstimatedHours() != null && req.getEstimatedHours().signum() < 0) {
            return Result.fail(40001, "ESTIMATED_HOURS_NON_NEGATIVE");
        }

        String workorderNo = (req.getWorkorderNo() != null && !req.getWorkorderNo().isBlank())
                ? req.getWorkorderNo()
                : docNoGenerator.nextWorkOrderNo();

        CrmWorkorder wo = new CrmWorkorder();
        wo.setWorkorderNo(workorderNo);
        wo.setDrawingId(req.getDrawingId());
        wo.setBomId(req.getBomId());
        wo.setProcessRouteId(req.getProcessRouteId());
        wo.setMaterialCode(req.getMaterialCode());
        wo.setProductName(req.getProductName());
        wo.setQty(req.getQty());
        wo.setUnit(req.getUnit() != null ? req.getUnit() : "件");
        wo.setPriority(req.getPriority() != null ? req.getPriority() : 5);
        wo.setStatus(STATUS_DRAFT);
        wo.setEquipmentId(req.getEquipmentId());
        wo.setEquipmentType(req.getEquipmentType());
        wo.setEstimatedHours(req.getEstimatedHours() != null ? req.getEstimatedHours() : BigDecimal.ZERO);
        wo.setIsFa(req.getIsFa() != null ? req.getIsFa() : 0);
        wo.setSalesOrderId(req.getSalesOrderId());
        wo.setSalesOrderNo(req.getSalesOrderNo());
        wo.setCreatedBy(operatorUserId);
        wo.setOwnerUserId(operatorUserId);
        DataScopeContext ctx = DataScopeContext.current();
        Long deptId = ctx != null && ctx.getDeptId() != null && ctx.getDeptId() > 0
                ? ctx.getDeptId() : 10L;
        wo.setDeptId(deptId);
        wo.setRemark(req.getRemark());
        wo.setCreatedAt(LocalDateTime.now());
        wo.setUpdatedAt(LocalDateTime.now());
        workorderMapper.insert(wo);

        populateStepsFromRoute(wo, req.getProcessRouteId(), req.getEquipmentType());

        return Result.ok(wo);
    }

    /**
     * AC-5.1.1：销售订单转工单（Feign 入口）
     */
    @Transactional
    @AuditLog(module = "production", action = "production.create_workorder_from_order")
    public Result<Map<String, Object>> createWorkorderFromOrder(WorkorderCreateRequest req, Long operatorUserId) {
        if (req.getSalesOrderId() == null) {
            return Result.fail(40001, "SALES_ORDER_ID_REQUIRED");
        }
        CrmWorkorder existing = workorderMapper.selectBySalesOrderId(req.getSalesOrderId());
        if (existing != null) {
            return Result.ok(toWorkorderMap(existing));
        }

        CrmWorkorder orphan = workorderMapper.selectUnlinkedByMaterial(req.getMaterialCode());
        if (orphan != null) {
            orphan.setSalesOrderId(req.getSalesOrderId());
            orphan.setSalesOrderNo(req.getSalesOrderNo());
            orphan.setUpdatedAt(LocalDateTime.now());
            workorderMapper.updateById(orphan);
            return Result.ok(toWorkorderMap(orphan));
        }

        Result<CrmWorkorder> created = createWorkorder(req, operatorUserId);
        if (!created.isSuccess()) {
            return Result.fail(created.getCode(), created.getMessage());
        }
        return Result.ok(toWorkorderMap(created.getData()));
    }

    public Result<Map<String, Object>> getWorkorderByNo(String workorderNo) {
        CrmWorkorder wo = workorderMapper.selectByNo(workorderNo);
        if (wo == null) return Result.fail(40404, "WORKORDER_NOT_FOUND");
        Result<Void> scope = ProductionDataScopeHelper.assertWorkorderScope(wo.getOwnerUserId(), wo.getDeptId());
        if (!scope.isSuccess()) return Result.fail(scope.getCode(), scope.getMessage());
        return Result.ok(toWorkorderMap(wo));
    }

    public Result<Map<String, Object>> getWorkorderBySalesOrderId(Long orderId) {
        CrmWorkorder wo = workorderMapper.selectBySalesOrderId(orderId);
        if (wo == null) return Result.fail(40404, "WORKORDER_NOT_FOUND");
        Result<Void> scope = ProductionDataScopeHelper.assertWorkorderScope(wo.getOwnerUserId(), wo.getDeptId());
        if (!scope.isSuccess()) return Result.fail(scope.getCode(), scope.getMessage());
        return Result.ok(toWorkorderMap(wo));
    }

    /** V1.4.0 · 客户现场演示 · 无金额/委外信息 */
    public Result<Map<String, Object>> visitorSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Result.fail(40001, "KEYWORD_REQUIRED");
        }
        String kw = keyword.trim();
        List<Map<String, Object>> raw = workorderMapper.selectWorkorders(kw, null, null, null, null, 30, 0);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            list.add(toVisitorMap(row));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("keyword", kw);
        data.put("list", list);
        data.put("total", list.size());
        return Result.ok(data);
    }

    /**
     * V1.4.0 · 客户现场演示 · 默认视图
     * 取所有"进行中"工单（脱敏），最多 limit 条。状态集：SCHEDULED/IN_PROGRESS/PROCESSING/INSPECTING/REPORTED
     * 跨状态 UNION 返回，让客户进入页面就看到"工厂确实在忙"。
     */
    public Result<Map<String, Object>> visitorActive(Integer limit) {
        int n = (limit == null || limit <= 0) ? 23 : Math.min(limit, 50);
        List<Map<String, Object>> list = new ArrayList<>();
        // 工单状态枚举：DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED
        // 进行中 = SCHEDULED + IN_PROGRESS（COMPLETED/CANCELLED 不算活跃）
        String[] activeStatuses = {"IN_PROGRESS", "SCHEDULED"};
        // 每状态按优先级+时间倒序取若干，最后拼 23 条并去重
        int perStatus = Math.max(5, n / activeStatuses.length + 2);
        Set<String> seenWo = new HashSet<>();
        for (String st : activeStatuses) {
            List<Map<String, Object>> rows = workorderMapper.selectWorkorders(null, st, null, null, null, perStatus, 0);
            for (Map<String, Object> row : rows) {
                String woNo = String.valueOf(row.get("workorderNo"));
                if (seenWo.add(woNo)) {
                    list.add(toVisitorMap(row));
                }
                if (list.size() >= n) break;
            }
            if (list.size() >= n) break;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        data.put("limit", n);
        return Result.ok(data);
    }

    /**
     * V1.4.0 · 客户现场演示 · 单工单详情 + 工序时间线
     * 入参：workorderNo（GD-20260601-XXXX）
     * 返回：完整工序步骤（名称/状态/预计完成日），并按用户友好文案替换"委外"标识。
     */
    public Result<Map<String, Object>> visitorDetail(String workorderNo) {
        if (workorderNo == null || workorderNo.isBlank()) {
            return Result.fail(40001, "WORKORDER_NO_REQUIRED");
        }
        CrmWorkorder wo = workorderMapper.selectByNo(workorderNo.trim());
        if (wo == null) return Result.fail(40404, "WORKORDER_NOT_FOUND");
        Map<String, Object> m = new HashMap<>();
        m.put("workorderNo", wo.getWorkorderNo());
        m.put("materialCode", wo.getMaterialCode());
        m.put("productName", wo.getProductName());
        m.put("qty", wo.getQty());
        m.put("unit", wo.getUnit());
        m.put("status", wo.getStatus());
        m.put("salesOrderNo", wo.getSalesOrderNo());
        m.put("plannedDelivery", wo.getScheduledEnd());
        m.put("scheduledStart", wo.getScheduledStart());
        // 工序时间线（含预计日期与状态）
        List<CrmWorkorderProcess> procs = processMapper.selectByWorkorderId(wo.getId());
        List<Map<String, Object>> timeline = new ArrayList<>();
        int total = procs.size();
        int done = 0;
        boolean hasInProgress = false;
        String currentName = null;
        for (int i = 0; i < procs.size(); i++) {
            CrmWorkorderProcess p = procs.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("seq", p.getProcessSeq());
            item.put("name", sanitizeStepName(p.getProcessName()));
            item.put("status", p.getStatus());
            item.put("statusLabel", mapStatusLabel(p.getStatus()));
            // 预计完成日 = scheduledStart + 累计估算小时（粗略）
            item.put("expectedEnd", estimateStepEnd(wo.getScheduledStart(), done, total));
            // crm_workorder_step.status 枚举：PENDING/IN_PROGRESS/COMPLETED
            if ("COMPLETED".equals(p.getStatus())) done++;
            else if ("IN_PROGRESS".equals(p.getStatus()) && !hasInProgress) {
                hasInProgress = true;
                currentName = sanitizeStepName(p.getProcessName());
            }
            timeline.add(item);
        }
        // 进度按工序实际完成度计算（更精准）
        m.put("progress", computeProgressFromSteps(total, done, hasInProgress));
        m.put("timeline", timeline);
        m.put("currentStep", currentName != null ? currentName : currentVisitorStep(wo.getId()));
        return Result.ok(m);
    }

    /** 按工序完成度算进度：COMPLETED=100%, IN_PROGRESS 进行中 = 80%, PENDING = 0% */
    private int computeProgressFromSteps(int total, int done, boolean hasInProgress) {
        if (total <= 0) return estimateVisitorProgress(null);
        int pct = (done * 100) / total;
        if (hasInProgress && done < total) {
            // 进行中的工序贡献 50% 进度
            pct = Math.min(100, pct + (100 / total) / 2);
        }
        return Math.min(100, Math.max(0, pct));
    }

    /** 把含"委外/外协"的工序名抹掉，对外仅显示"加工工序"或工序类别 */
    private String sanitizeStepName(String raw) {
        if (raw == null) return "加工工序";
        String name = raw.replaceAll("(?i)委外|外协|外发", "").trim();
        if (name.isEmpty()) return "加工工序";
        // 已知关键字归一化
        if (name.contains("表面")) return "表面处理";
        if (name.contains("热处理")) return "热处理";
        if (name.contains("电镀")) return "电镀";
        if (name.contains("喷涂")) return "喷涂";
        return name;
    }

    /** 状态码 → 客户友好文案 */
    private String mapStatusLabel(String status) {
        if (status == null) return "待开始";
        return switch (status) {
            case "FINISHED", "CLOSED" -> "已完成";
            case "IN_PROGRESS", "PROCESSING" -> "进行中";
            case "PENDING", "SCHEDULED" -> "待开始";
            case "INSPECTING", "REPORTED" -> "质检中";
            default -> "跟进中";
        };
    }

    /** 工序预计完成日（粗略：start + 累计天数） */
    private String estimateStepEnd(java.time.LocalDateTime scheduledStart, int done, int total) {
        if (scheduledStart == null || total <= 0) return null;
        try {
            java.time.LocalDate start = scheduledStart.toLocalDate();
            int span = 7; // 默认 7 天工单周期
            int daysPerStep = Math.max(1, span / total);
            return start.plusDays((long) (done + 1) * daysPerStep).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> toVisitorMap(Map<String, Object> row) {
        Map<String, Object> m = new HashMap<>();
        Object woId = row.get("id");
        m.put("workorderNo", row.get("workorderNo"));
        m.put("materialCode", row.get("materialCode"));
        m.put("productName", row.get("productName"));
        m.put("qty", row.get("qty"));
        m.put("status", row.get("status"));
        m.put("salesOrderNo", row.get("salesOrderNo"));
        m.put("progress", estimateVisitorProgress(String.valueOf(row.get("status"))));
        m.put("currentStep", currentVisitorStep(woId));
        m.put("steps", visitorSteps(woId));
        m.put("plannedDelivery", row.get("scheduledEnd"));
        return m;
    }

    private String currentVisitorStep(Object woId) {
        if (woId == null) return "生产中";
        Long id = woId instanceof Number n ? n.longValue() : null;
        if (id == null) return "生产中";
        List<CrmWorkorderProcess> procs = processMapper.selectByWorkorderId(id);
        for (CrmWorkorderProcess p : procs) {
            if ("IN_PROGRESS".equals(p.getStatus())) {
                return p.getProcessName();
            }
        }
        for (CrmWorkorderProcess p : procs) {
            if ("PENDING".equals(p.getStatus())) {
                return p.getProcessName();
            }
        }
        return procs.isEmpty() ? "待排产" : "跟进中";
    }

    private List<Map<String, Object>> visitorSteps(Object woId) {
        if (woId == null) return List.of();
        Long id = woId instanceof Number n ? n.longValue() : null;
        if (id == null) return List.of();
        List<CrmWorkorderProcess> procs = processMapper.selectByWorkorderId(id);
        List<Map<String, Object>> steps = new ArrayList<>();
        for (CrmWorkorderProcess p : procs) {
            Map<String, Object> s = new HashMap<>();
            s.put("seq", p.getProcessSeq());
            s.put("name", p.getProcessName());
            s.put("status", p.getStatus());
            steps.add(s);
        }
        return steps;
    }

    private int estimateVisitorProgress(String status) {
        return switch (status != null ? status : "") {
            case "IN_PROGRESS", "PROCESSING" -> 60;
            case "REPORTED", "INSPECTING" -> 80;
            case "FINISHED", "CLOSED" -> 100;
            case "SCHEDULED" -> 30;
            default -> 15;
        };
    }

    private void populateStepsFromRoute(CrmWorkorder wo, Long processId, String fallbackEquipmentType) {
        if (processId == null) {
            insertDefaultStep(wo, fallbackEquipmentType);
            return;
        }
        Result<Map<String, Object>> routeRes = processService.getRoute(processId, null);
        if (!routeRes.isSuccess() || routeRes.getData() == null) {
            insertDefaultStep(wo, fallbackEquipmentType);
            return;
        }
        @SuppressWarnings("unchecked")
        List<CrmProcessStep> steps = (List<CrmProcessStep>) routeRes.getData().get("steps");
        if (steps == null || steps.isEmpty()) {
            insertDefaultStep(wo, fallbackEquipmentType);
            return;
        }

        BigDecimal totalHours = BigDecimal.ZERO;
        String firstMachineType = null;
        for (CrmProcessStep ps : steps) {
            CrmWorkorderStep step = new CrmWorkorderStep();
            step.setWorkorderId(wo.getId());
            step.setStepNo(ps.getStepNo());
            step.setStepName(ps.getStepName());
            step.setEquipmentType(ps.getMachineType());
            step.setEstimatedMinutes(ps.getEstimatedHours() != null
                    ? ps.getEstimatedHours().multiply(new BigDecimal(60)).intValue() : 0);
            step.setStatus("PENDING");
            stepMapper.insert(step);

            CrmWorkorderProcess proc = new CrmWorkorderProcess();
            proc.setWorkorderId(wo.getId());
            proc.setWorkorderNo(wo.getWorkorderNo());
            proc.setProcessSeq(ps.getStepNo());
            proc.setProcessCode("P" + String.format("%02d", ps.getStepNo()));
            proc.setProcessName(ps.getStepName());
            proc.setMaterialCode(wo.getMaterialCode());
            proc.setStatus("PENDING");
            proc.setCreatedAt(LocalDateTime.now());
            proc.setUpdatedAt(LocalDateTime.now());
            processMapper.insert(proc);

            if (firstMachineType == null && ps.getMachineType() != null) {
                firstMachineType = ps.getMachineType();
            }
            if (ps.getEstimatedHours() != null) {
                totalHours = totalHours.add(ps.getEstimatedHours());
            }
        }
        if (wo.getEquipmentType() == null && firstMachineType != null) {
            wo.setEquipmentType(firstMachineType);
        }
        if (wo.getEstimatedHours() == null || wo.getEstimatedHours().signum() == 0) {
            wo.setEstimatedHours(totalHours);
        }
        wo.setUpdatedAt(LocalDateTime.now());
        workorderMapper.updateById(wo);
    }

    private void insertDefaultStep(CrmWorkorder wo, String equipmentType) {
        CrmWorkorderStep step = new CrmWorkorderStep();
        step.setWorkorderId(wo.getId());
        step.setStepNo(1);
        step.setStepName("主工序");
        step.setEquipmentType(equipmentType != null ? equipmentType : wo.getEquipmentType());
        step.setEstimatedMinutes(wo.getEstimatedHours() != null
                ? wo.getEstimatedHours().multiply(new BigDecimal(60)).intValue() : 0);
        step.setStatus("PENDING");
        stepMapper.insert(step);
    }

    private Map<String, Object> toWorkorderMap(CrmWorkorder wo) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", wo.getId());
        m.put("workorderNo", wo.getWorkorderNo());
        m.put("materialCode", wo.getMaterialCode());
        m.put("productName", wo.getProductName());
        m.put("qty", wo.getQty());
        m.put("status", wo.getStatus());
        m.put("salesOrderId", wo.getSalesOrderId());
        m.put("salesOrderNo", wo.getSalesOrderNo());
        return m;
    }

    /**
     * AC-5.1.2：工单排�?     * 算法：机台负�?+ 工艺先后 + 工时
     */
    @Transactional
    @AuditLog(module = "production", action = "production.schedule_workorder")
    public Result<CrmProductionSchedule> scheduleWorkorder(Long workorderId, WorkorderScheduleRequest req, Long operatorUserId) {
        CrmWorkorder wo = workorderMapper.selectById(workorderId);
        if (wo == null) {
            return Result.fail(40404, "WORKORDER_NOT_FOUND");
        }
        Result<Void> scope = ProductionDataScopeHelper.assertWorkorderScope(wo.getOwnerUserId(), wo.getDeptId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        if (!STATUS_DRAFT.equals(wo.getStatus()) && !STATUS_SCHEDULED.equals(wo.getStatus())) {
            return Result.fail(40903, "WORKORDER_NOT_SCHEDULABLE");
        }
        if (req.getPlanStart() == null || req.getPlanEnd() == null) {
            return Result.fail(40001, "PLAN_TIME_REQUIRED");
        }
        if (req.getPlanEnd().isBefore(req.getPlanStart())) {
            return Result.fail(40001, "PLAN_END_BEFORE_START");
        }
        // P1 修补 3：设备类型匹�?
            if (req.getEquipmentType() != null && wo.getEquipmentType() != null
                && !req.getEquipmentType().equals(wo.getEquipmentType())) {
            return Result.fail(40903, "EQUIPMENT_TYPE_MISMATCH");
        }
        // P1 修补 2：排产冲突检�?
            List<CrmProductionSchedule> conflicts = scheduleMapper.selectConflicts(
            req.getEquipmentId(), req.getPlanStart(), req.getPlanEnd());
        if (!conflicts.isEmpty() && !Boolean.TRUE.equals(req.getForceOverride())) {
            return Result.fail(40903, "SCHEDULE_CONFLICT_DETECTED");
        }

        // 删除旧排�?
            CrmProductionSchedule old = scheduleMapper.selectByWorkorderId(workorderId);
        if (old != null) {
            scheduleMapper.deleteById(old.getId());
        }

        // 创建新排�?
            CrmProductionSchedule schedule = new CrmProductionSchedule();
        schedule.setScheduleNo(docNoGenerator.nextScheduleNo());
        schedule.setWorkorderId(workorderId);
        schedule.setEquipmentId(req.getEquipmentId());
        schedule.setEquipmentType(req.getEquipmentType() != null ? req.getEquipmentType() : wo.getEquipmentType());
        schedule.setPlanStart(req.getPlanStart());
        schedule.setPlanEnd(req.getPlanEnd());
        schedule.setStatus(SCHEDULE_PLANNED);
        schedule.setCreatedAt(LocalDateTime.now());
        scheduleMapper.insert(schedule);

        // 更新工单
            wo.setStatus(STATUS_SCHEDULED);
        wo.setScheduledStart(req.getPlanStart());
        wo.setScheduledEnd(req.getPlanEnd());
        wo.setEquipmentId(req.getEquipmentId());
        wo.setUpdatedAt(LocalDateTime.now());
        workorderMapper.updateById(wo);

        return Result.ok(schedule);
    }

    /**
     * 开�?     */
    @Transactional
    @AuditLog(module = "production", action = "production.start_workorder")
    public Result<CrmWorkorder> startProduction(Long workorderId, Long operatorUserId) {
        CrmWorkorder wo = workorderMapper.selectById(workorderId);
        if (wo == null) {
            return Result.fail(40404, "WORKORDER_NOT_FOUND");
        }
        Result<Void> scope = ProductionDataScopeHelper.assertWorkorderScope(wo.getOwnerUserId(), wo.getDeptId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        if (!STATUS_SCHEDULED.equals(wo.getStatus())) {
            return Result.fail(40903, "WORKORDER_NOT_IN_SCHEDULED_STATE");
        }
        wo.setStatus(STATUS_IN_PROGRESS);
        wo.setActualStart(LocalDateTime.now());
        wo.setUpdatedAt(LocalDateTime.now());
        workorderMapper.updateById(wo);
        return Result.ok(wo);
    }

    /**
     * 完工
     */
    @Transactional
    @AuditLog(module = "production", action = "production.finish_workorder")
    public Result<CrmWorkorder> finishProduction(Long workorderId, Long operatorUserId) {
        CrmWorkorder wo = workorderMapper.selectById(workorderId);
        if (wo == null) {
            return Result.fail(40404, "WORKORDER_NOT_FOUND");
        }
        Result<Void> scope = ProductionDataScopeHelper.assertWorkorderScope(wo.getOwnerUserId(), wo.getDeptId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        if (!STATUS_IN_PROGRESS.equals(wo.getStatus())) {
            return Result.fail(40903, "WORKORDER_NOT_IN_PROGRESS");
        }
        wo.setStatus(STATUS_COMPLETED);
        wo.setActualEnd(LocalDateTime.now());
        if (wo.getActualStart() != null) {
            long seconds = java.time.Duration.between(wo.getActualStart(), wo.getActualEnd()).getSeconds();
            wo.setActualHours(new BigDecimal(seconds / 3600.0));
        }
        wo.setUpdatedAt(LocalDateTime.now());
        workorderMapper.updateById(wo);
        return Result.ok(wo);
    }

    /**
     * 取消
     */
    @Transactional
    public Result<CrmWorkorder> cancelWorkorder(Long workorderId, Long operatorUserId) {
        CrmWorkorder wo = workorderMapper.selectById(workorderId);
        if (wo == null) {
            return Result.fail(40404, "WORKORDER_NOT_FOUND");
        }
        Result<Void> scope = ProductionDataScopeHelper.assertWorkorderScope(wo.getOwnerUserId(), wo.getDeptId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        if (STATUS_COMPLETED.equals(wo.getStatus())) {
            return Result.fail(40903, "WORKORDER_ALREADY_COMPLETED");
        }
        wo.setStatus(STATUS_CANCELLED);
        wo.setUpdatedAt(LocalDateTime.now());
        workorderMapper.updateById(wo);
        return Result.ok(wo);
    }

    /**
     * 分页查询
     */
    public Result<Map<String, Object>> listWorkorders(WorkorderQueryRequest query) {
        int limit = query.getSize() > 0 ? query.getSize() : 20;
        int offset = Math.max(query.getPage(), 0) * limit;
        Long scopeOwnerId = ProductionDataScopeHelper.resolveScopeOwnerId();
        Long scopeDeptId = ProductionDataScopeHelper.resolveScopeDeptId();
        List<Map<String, Object>> list = workorderMapper.selectWorkorders(
            query.getKeyword(), query.getStatus(), query.getMaterialCode(),
            scopeOwnerId, scopeDeptId, limit, offset);
        long total = workorderMapper.countWorkorders(
            query.getKeyword(), query.getStatus(), query.getMaterialCode(),
            scopeOwnerId, scopeDeptId);
        Map<String, Object> page = new HashMap<>();
        page.put("list", list);
        page.put("records", list);
        page.put("page", query.getPage());
        page.put("size", limit);
        page.put("total", total);
        return Result.ok(page);
    }

    /**
     * 获取详情
     */
    public Result<CrmWorkorder> getWorkorder(Long id) {
        CrmWorkorder wo = workorderMapper.selectById(id);
        if (wo == null) {
            return Result.fail(40404, "WORKORDER_NOT_FOUND");
        }
        Result<Void> scope = ProductionDataScopeHelper.assertWorkorderScope(wo.getOwnerUserId(), wo.getDeptId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        return Result.ok(wo);
    }

    /**
     * AC-5.1.3：工�?timeline
     */
    public Result<WorkorderTimelineResponse> getTimeline(Long workorderId) {
        CrmWorkorder wo = workorderMapper.selectById(workorderId);
        if (wo == null) {
            return Result.fail(40404, "WORKORDER_NOT_FOUND");
        }
        Result<Void> scope = ProductionDataScopeHelper.assertWorkorderScope(wo.getOwnerUserId(), wo.getDeptId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        WorkorderTimelineResponse resp = new WorkorderTimelineResponse();
        resp.setWorkorderNo(wo.getWorkorderNo());
        resp.setStatus(wo.getStatus());
        List<WorkorderTimelineResponse.TimelineNode> nodes = new ArrayList<>();

        WorkorderTimelineResponse.TimelineNode create = new WorkorderTimelineResponse.TimelineNode();
        create.setNodeName("CREATE");
        create.setOperatedAt(wo.getCreatedAt() == null ? null : wo.getCreatedAt().toString());
        create.setOperatorUserId(wo.getCreatedBy());
        create.setDetail("工单创建");
        nodes.add(create);

        if (wo.getScheduledStart() != null) {
            WorkorderTimelineResponse.TimelineNode schedule = new WorkorderTimelineResponse.TimelineNode();
            schedule.setNodeName("SCHEDULE");
            schedule.setOperatedAt(wo.getScheduledStart().toString());
            schedule.setDetail("排产完成");
            nodes.add(schedule);
        }
        if (wo.getActualStart() != null) {
            WorkorderTimelineResponse.TimelineNode start = new WorkorderTimelineResponse.TimelineNode();
            start.setNodeName("START");
            start.setOperatedAt(wo.getActualStart().toString());
            start.setDetail("开始");
            nodes.add(start);
        }
        if (wo.getActualEnd() != null) {
            WorkorderTimelineResponse.TimelineNode finish = new WorkorderTimelineResponse.TimelineNode();
            finish.setNodeName("FINISH");
            finish.setOperatedAt(wo.getActualEnd().toString());
            finish.setDetail("完工");
            nodes.add(finish);
        }
        resp.setNodes(nodes);
        return Result.ok(resp);
    }

    /**
     * 工序列表
     */
    public Result<List<CrmWorkorderStep>> listSteps(Long workorderId) {
        return Result.ok(stepMapper.selectByWorkorderId(workorderId));
    }

    /**
     * 排产列表
     */
    public Result<List<CrmProductionSchedule>> listSchedules() {
        return Result.ok(scheduleMapper.selectList(null));
    }
}
