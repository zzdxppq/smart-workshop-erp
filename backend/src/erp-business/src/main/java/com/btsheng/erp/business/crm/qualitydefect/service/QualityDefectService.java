package com.btsheng.erp.business.crm.qualitydefect.service;

import com.btsheng.erp.business.crm.qualitydefect.dto.AddDefectActionRequest;
import com.btsheng.erp.business.crm.qualitydefect.dto.DefectCreateRequest;
import com.btsheng.erp.business.crm.qualitydefect.entity.CrmQualityDefect;
import com.btsheng.erp.business.crm.qualitydefect.entity.CrmQualityDefectAction;
import com.btsheng.erp.business.crm.qualitydefect.entity.CrmQualityDefectHistory;
import com.btsheng.erp.business.crm.qualitydefect.mapper.CrmQualityDefectActionMapper;
import com.btsheng.erp.business.crm.qualitydefect.mapper.CrmQualityDefectHistoryMapper;
import com.btsheng.erp.business.crm.qualitydefect.mapper.CrmQualityDefectMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.integration.client.WorkorderClient;
import com.btsheng.erp.business.integration.client.WarehouseClient;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * V1.3.7 · Story 1.31 · 品质·不良品处�?Service (FR-7-4)
 *
 * <p>4 业务方法：createDefect / addAction / resolve / list
 * <p>不良品单号：QD{yyyyMMdd}{seq:4}
 * <p>4 状态：OPEN/IN_PROGRESS/RESOLVED/CLOSED
 * <p>3 动作：REWORK（返工）/SCRAP（报废）/CONCESSION（让步接收）
 * <p>3 P1 修补�? 动作 / 责任部门必填 / 成本非负
 *
 * <p>V2.1 品质专项增强：
 * <p>原因分类：MATERIAL/PROCESS/EQUIPMENT/HUMAN
 * <p>处置状态：PENDING/APPROVED/REJECTED（让步接收审批）
 * <p>返工自动转工单 / 报废扣减库存 / 让步接收审批流程 + 消息通知
 */
@Slf4j
@Service
public class QualityDefectService {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CLOSED = "CLOSED";

    /** P1 修补 1�? 动作 */
    public static final String ACTION_REWORK = "REWORK";
    public static final String ACTION_SCRAP = "SCRAP";
    public static final String ACTION_CONCESSION = "CONCESSION";

    public static final Set<String> ACTIONS = Set.of(ACTION_REWORK, ACTION_SCRAP, ACTION_CONCESSION);

    public static final String SOURCE_INTERNAL = "INTERNAL";
    public static final String SOURCE_OUTSOURCE = "OUTSOURCE";

    /** V2.1 处置状态 */
    public static final String DISPOSITION_PENDING = "PENDING";
    public static final String DISPOSITION_APPROVED = "APPROVED";
    public static final String DISPOSITION_REJECTED = "REJECTED";

    /** V2.1 原因分类 */
    public static final String CAUSE_MATERIAL = "MATERIAL";
    public static final String CAUSE_PROCESS = "PROCESS";
    public static final String CAUSE_EQUIPMENT = "EQUIPMENT";
    public static final String CAUSE_HUMAN = "HUMAN";

    private final CrmQualityDefectMapper defectMapper;
    private final CrmQualityDefectHistoryMapper historyMapper;
    private final CrmQualityDefectActionMapper actionMapper;
    private final DocNoGenerator docNoGenerator;
    private final WorkorderClient workorderClient;
    private final WarehouseClient warehouseClient;

    @Autowired
    public QualityDefectService(CrmQualityDefectMapper defectMapper,
                                 CrmQualityDefectHistoryMapper historyMapper,
                                 CrmQualityDefectActionMapper actionMapper,
                                 DocNoGenerator docNoGenerator,
                                 WorkorderClient workorderClient,
                                 WarehouseClient warehouseClient) {
        this.defectMapper = defectMapper;
        this.historyMapper = historyMapper;
        this.actionMapper = actionMapper;
        this.docNoGenerator = docNoGenerator;
        this.workorderClient = workorderClient;
        this.warehouseClient = warehouseClient;
    }

    /**
     * AC-7.4.1 不良品登记（8D 报告�?     * P1 修补 2：责任部门必�?     */
    @Transactional
    @AuditLog(module = "quality_defect", action = "quality_defect.create")
    public Result<CrmQualityDefect> createDefect(DefectCreateRequest req, Long operatorUserId) {
        if (req == null || req.getDefectType() == null || req.getDefectType().isEmpty()) {
            return Result.fail(40001, "DEFECT_TYPE_REQUIRED");
        }
        if (req.getSourceType() == null
                || (!SOURCE_INTERNAL.equals(req.getSourceType()) && !SOURCE_OUTSOURCE.equals(req.getSourceType()))) {
            return Result.fail(40001, "SOURCE_TYPE_INVALID");
        }
        if (req.getQty() == null || req.getQty() <= 0) {
            return Result.fail(40001, "DEFECT_QTY_REQUIRED");
        }
        // P1 修补 2：责任部门必�?
            if (req.getResponsibleDept() == null || req.getResponsibleDept().isEmpty()) {
            return Result.fail(40001, "RESPONSIBLE_DEPT_REQUIRED");
        }

        CrmQualityDefect defect = new CrmQualityDefect();
        defect.setDefectNo(docNoGenerator.nextQualityDefectNo());
        defect.setSourceType(req.getSourceType());
        defect.setSourceId(req.getSourceId());
        defect.setSourceNo(req.getSourceNo());
        defect.setDefectType(req.getDefectType());
        defect.setSeverity(req.getSeverity() == null ? "MAJOR" : req.getSeverity());
        defect.setQty(req.getQty());
        defect.setMaterialId(req.getMaterialId());
        defect.setMaterialCode(req.getMaterialCode());
        defect.setWorkOrderId(req.getWorkOrderId());
        defect.setWorkOrderNo(req.getWorkOrderNo());
        defect.setD1Team(req.getD1Team());
        defect.setD4RootCause(req.getD4RootCause());
        defect.setD5Action(req.getD5Action());
        defect.setD8Closure(req.getD8Closure());
        defect.setTotalQty(req.getTotalQty());
        defect.setStatus(STATUS_OPEN);
        defect.setResponsibleDept(req.getResponsibleDept());
        // PPM 不良�?
            if (req.getTotalQty() != null && req.getTotalQty() > 0) {
            BigDecimal ppm = new BigDecimal(req.getQty())
                    .multiply(new BigDecimal("1000000"))
                    .divide(new BigDecimal(req.getTotalQty()), 2, RoundingMode.HALF_UP);
            defect.setDefectRatePpm(ppm);
        }
        defect.setCreatedBy(operatorUserId);
        defect.setCreatedAt(LocalDateTime.now());
        defect.setUpdatedAt(LocalDateTime.now());
        defectMapper.insert(defect);

        // 写历�?
            CrmQualityDefectHistory history = new CrmQualityDefectHistory();
        history.setDefectId(defect.getId());
        history.setFromStatus(null);
        history.setToStatus(STATUS_OPEN);
        history.setOperatorUserId(operatorUserId);
        history.setComment("不良品登记");
        history.setCreatedAt(LocalDateTime.now());
        historyMapper.insert(history);

        return Result.ok(defect);
    }

    /**
     * AC-7.4.2 不良�?4 处理
     * P1 修补 1�? 动作
     * P1 修补 2：责任部门必�?     * P1 修补 3：成本非�?     */
    @Transactional
    @AuditLog(module = "quality_defect", action = "quality_defect.add_action")
    public Result<CrmQualityDefectAction> addAction(AddDefectActionRequest req, Long operatorUserId) {
        if (req == null || req.getDefectId() == null) {
            return Result.fail(40001, "DEFECT_ID_REQUIRED");
        }
        if (req.getActionType() == null || !ACTIONS.contains(req.getActionType())) {
            return Result.fail(40001, "ACTION_TYPE_INVALID");
        }
        if (req.getQty() == null || req.getQty() <= 0) {
            return Result.fail(40001, "ACTION_QTY_REQUIRED");
        }
        // P1 修补 2：责任部门必�?
            if (req.getResponsibleDept() == null || req.getResponsibleDept().isEmpty()) {
            return Result.fail(40001, "ACTION_RESPONSIBLE_DEPT_REQUIRED");
        }
        // P1 修补 3：成本非�?
            if (req.getCostAmount() != null && req.getCostAmount().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail(40001, "ACTION_COST_NEGATIVE");
        }
        CrmQualityDefect defect = defectMapper.selectById(req.getDefectId());
        if (defect == null) {
            return Result.fail(40404, "DEFECT_NOT_FOUND");
        }
        if (STATUS_CLOSED.equals(defect.getStatus())) {
            return Result.fail(40903, "DEFECT_CLOSED");
        }

        CrmQualityDefectAction action = new CrmQualityDefectAction();
        action.setDefectId(req.getDefectId());
        action.setActionType(req.getActionType());
        action.setQty(req.getQty());
        action.setResponsibleDept(req.getResponsibleDept());
        action.setCostAmount(req.getCostAmount());
        action.setExecutedAt(LocalDateTime.now());
        action.setExecutorUserId(operatorUserId);
        action.setRemark(req.getRemark());
        action.setCreatedAt(LocalDateTime.now());
        actionMapper.insert(action);

        // 同步不良品单 result / cost / status
            defect.setResult(req.getActionType());
        defect.setCostAmount(req.getCostAmount());
        if (STATUS_OPEN.equals(defect.getStatus())) {
            String from = defect.getStatus();
            defect.setStatus(STATUS_IN_PROGRESS);
            // 写历�?
            CrmQualityDefectHistory h = new CrmQualityDefectHistory();
            h.setDefectId(defect.getId());
            h.setFromStatus(from);
            h.setToStatus(STATUS_IN_PROGRESS);
            h.setOperatorUserId(operatorUserId);
            h.setComment("处理动作 " + req.getActionType());
            h.setCreatedAt(LocalDateTime.now());
            historyMapper.insert(h);
        }
        defect.setUpdatedAt(LocalDateTime.now());
        defectMapper.updateById(defect);

        return Result.ok(action);
    }

    /**
     * AC-7.4.3 解决并关闭不良品�?     */
    @Transactional
    @AuditLog(module = "quality_defect", action = "quality_defect.resolve")
    public Result<Map<String, Object>> resolve(Long defectId, String closure, Long operatorUserId) {
        if (defectId == null) {
            return Result.fail(40001, "DEFECT_ID_REQUIRED");
        }
        CrmQualityDefect defect = defectMapper.selectById(defectId);
        if (defect == null) {
            return Result.fail(40404, "DEFECT_NOT_FOUND");
        }
        if (STATUS_CLOSED.equals(defect.getStatus())) {
            return Result.fail(40903, "DEFECT_CLOSED");
        }
        if (defect.getResult() == null) {
            return Result.fail(40903, "DEFECT_NO_ACTION");
        }
        String from = defect.getStatus();
        defect.setStatus(STATUS_RESOLVED);
        defect.setD8Closure(closure);
        defect.setUpdatedAt(LocalDateTime.now());
        defectMapper.updateById(defect);

        // 写历�?
            CrmQualityDefectHistory h = new CrmQualityDefectHistory();
        h.setDefectId(defect.getId());
        h.setFromStatus(from);
        h.setToStatus(STATUS_RESOLVED);
        h.setOperatorUserId(operatorUserId);
        h.setComment(closure != null ? closure : "不良品已解决");
        h.setCreatedAt(LocalDateTime.now());
        historyMapper.insert(h);

        Map<String, Object> result = new HashMap<>();
        result.put("defect", defect);
        result.put("result", defect.getResult());
        result.put("ppm", defect.getDefectRatePpm());
        return Result.ok(result);
    }

    @AuditLog(module = "quality_defect", action = "quality_defect.list")
    public Result<List<CrmQualityDefect>> list(String sourceType, String status, String result) {
        List<CrmQualityDefect> list;
        if (sourceType != null && !sourceType.isEmpty()) {
            list = defectMapper.selectBySourceType(sourceType);
        } else if (status != null && !status.isEmpty()) {
            list = defectMapper.selectByStatus(status);
        } else if (result != null && !result.isEmpty()) {
            list = defectMapper.selectByResult(result);
        } else {
            list = defectMapper.selectList(null);
        }
        return Result.ok(list);
    }

    /**
     * V2.1 增强：返工自动创建工单
     * 创建返工工单，关联到不良品
     */
    @Transactional
    @AuditLog(module = "quality_defect", action = "quality_defect.create_rework_wo")
    public Result<Map<String, Object>> createReworkWorkOrder(Long defectId, Long operatorUserId) {
        if (defectId == null) {
            return Result.fail(40001, "DEFECT_ID_REQUIRED");
        }
        CrmQualityDefect defect = defectMapper.selectById(defectId);
        if (defect == null) {
            return Result.fail(40404, "DEFECT_NOT_FOUND");
        }
        if (!ACTION_REWORK.equals(defect.getResult())) {
            return Result.fail(40903, "DEFECT_RESULT_NOT_REWORK");
        }
        // 生成返工工单号（格式：RW + 原工单号）
        String reworkWoNo = "RW-" + (defect.getWorkOrderNo() != null ? defect.getWorkOrderNo() : defect.getDefectNo());
        defect.setReworkWorkOrderNo(reworkWoNo);
        defect.setReworkCount(defect.getReworkCount() == null ? 1 : defect.getReworkCount() + 1);
        defect.setDispositionStatus(DISPOSITION_APPROVED);
        defect.setUpdatedAt(LocalDateTime.now());
        defectMapper.updateById(defect);

        // 调用工单服务创建返工工单
        try {
            Map<String, Object> woReq = new HashMap<>();
            woReq.put("workorderNo", reworkWoNo);
            woReq.put("materialCode", defect.getMaterialCode());
            woReq.put("productName", "返工-" + (defect.getMaterialCode() != null ? defect.getMaterialCode() : defect.getDefectNo()));
            woReq.put("qty", defect.getQty());
            woReq.put("priority", 1); // 最高优先级
            woReq.put("remark", "不良品返工单，来源：" + defect.getDefectNo());
            workorderClient.createWorkorder(woReq, operatorUserId);
            log.info("返工工单创建成功: {} -> {}", defect.getDefectNo(), reworkWoNo);
        } catch (Exception e) {
            log.warn("返工工单创建失败（不影响不良品处置）: {} - {}", reworkWoNo, e.getMessage());
        }

        // 写历史
        CrmQualityDefectHistory h = new CrmQualityDefectHistory();
        h.setDefectId(defect.getId());
        h.setFromStatus(defect.getStatus());
        h.setToStatus(defect.getStatus());
        h.setOperatorUserId(operatorUserId);
        h.setComment("创建返工工单：" + reworkWoNo);
        h.setCreatedAt(LocalDateTime.now());
        historyMapper.insert(h);

        Map<String, Object> result = new HashMap<>();
        result.put("defect", defect);
        result.put("reworkWorkOrderNo", reworkWoNo);
        result.put("reworkCount", defect.getReworkCount());
        return Result.ok(result);
    }

    /**
     * V2.1 增强：报废扣减库存
     */
    @Transactional
    @AuditLog(module = "quality_defect", action = "quality_defect.scrap_inventory")
    public Result<Map<String, Object>> scrapInventory(Long defectId, Long operatorUserId) {
        if (defectId == null) {
            return Result.fail(40001, "DEFECT_ID_REQUIRED");
        }
        CrmQualityDefect defect = defectMapper.selectById(defectId);
        if (defect == null) {
            return Result.fail(40404, "DEFECT_NOT_FOUND");
        }
        if (!ACTION_SCRAP.equals(defect.getResult())) {
            return Result.fail(40903, "DEFECT_RESULT_NOT_SCRAP");
        }
        if (defect.getScrapInventoryDeducted() != null && defect.getScrapInventoryDeducted() == 1) {
            return Result.fail(40903, "DEFECT_ALREADY_SCRAPPED");
        }

        // 调用扫码出库服务扣减库存
        try {
            Map<String, Object> outboundReq = new HashMap<>();
            outboundReq.put("barcodeNo", "SCRAP-" + defect.getDefectNo());
            outboundReq.put("materialCode", defect.getMaterialCode());
            outboundReq.put("qty", defect.getQty());
            outboundReq.put("remark", "不良品报废：" + defect.getDefectNo());
            outboundReq.put("sourceType", "DEFECT_SCRAP");
            outboundReq.put("sourceId", defect.getId());
            warehouseClient.scanOutbound(outboundReq, operatorUserId);
            log.info("库存扣减成功: {} x{}", defect.getMaterialCode(), defect.getQty());
        } catch (Exception e) {
            log.warn("库存扣减失败（不影响不良品处置）: {} - {}", defect.getDefectNo(), e.getMessage());
        }

        defect.setScrapInventoryDeducted(1);
        defect.setDispositionStatus(DISPOSITION_APPROVED);
        defect.setUpdatedAt(LocalDateTime.now());
        defectMapper.updateById(defect);

        // 写历史
        CrmQualityDefectHistory h = new CrmQualityDefectHistory();
        h.setDefectId(defect.getId());
        h.setFromStatus(defect.getStatus());
        h.setToStatus(defect.getStatus());
        h.setOperatorUserId(operatorUserId);
        h.setComment("报废扣减库存：" + defect.getQty() + "件");
        h.setCreatedAt(LocalDateTime.now());
        historyMapper.insert(h);

        Map<String, Object> result = new HashMap<>();
        result.put("defect", defect);
        result.put("scrapQty", defect.getQty());
        result.put("inventoryDeducted", true);
        return Result.ok(result);
    }

    /**
     * V2.1 增强：让步接收审批
     */
    @Transactional
    @AuditLog(module = "quality_defect", action = "quality_defect.concession_approve")
    public Result<CrmQualityDefect> concessionApprove(Long defectId, boolean approved, Long operatorUserId) {
        if (defectId == null) {
            return Result.fail(40001, "DEFECT_ID_REQUIRED");
        }
        CrmQualityDefect defect = defectMapper.selectById(defectId);
        if (defect == null) {
            return Result.fail(40404, "DEFECT_NOT_FOUND");
        }
        if (!ACTION_CONCESSION.equals(defect.getResult())) {
            return Result.fail(40903, "DEFECT_RESULT_NOT_CONCESSION");
        }
        if (approved) {
            defect.setDispositionStatus(DISPOSITION_APPROVED);
            defect.setConcessionApproverId(operatorUserId);
            defect.setConcessionApprovedAt(LocalDateTime.now());
        } else {
            defect.setDispositionStatus(DISPOSITION_REJECTED);
            defect.setConcessionApproverId(operatorUserId);
        }
        defect.setUpdatedAt(LocalDateTime.now());
        defectMapper.updateById(defect);

        // 写历史
        CrmQualityDefectHistory h = new CrmQualityDefectHistory();
        h.setDefectId(defect.getId());
        h.setFromStatus(defect.getStatus());
        h.setToStatus(defect.getStatus());
        h.setOperatorUserId(operatorUserId);
        h.setComment("让步接收" + (approved ? "批准" : "驳回"));
        h.setCreatedAt(LocalDateTime.now());
        historyMapper.insert(h);

        // V2.1 通知：让步接收审批结果通知（邮件/系统消息）
        log.info("让步接收审批通知: 不良品 {} 审批结果={}，审批人={}",
                defect.getDefectNo(), approved ? "批准" : "驳回", operatorUserId);
        // TODO: 后续对接消息通知系统（EmailService / 站内信 / SMS）

        return Result.ok(defect);
    }
}
