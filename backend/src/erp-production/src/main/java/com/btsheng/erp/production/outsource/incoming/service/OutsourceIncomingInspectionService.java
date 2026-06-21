package com.btsheng.erp.production.outsource.incoming.service;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.incoming.dto.AddDefectRequest;
import com.btsheng.erp.production.outsource.incoming.dto.IncomingInspectionRequest;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingDefect;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingInspection;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingItem;
import com.btsheng.erp.production.outsource.incoming.mapper.CrmOutsourceIncomingDefectMapper;
import com.btsheng.erp.production.outsource.incoming.mapper.CrmOutsourceIncomingInspectionMapper;
import com.btsheng.erp.production.outsource.incoming.mapper.CrmOutsourceIncomingItemMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.25 · 委外来料质检 Service (FR-6-5)
 *
 * <p>5 业务方法：createInspection / addDefect / pass / reject / list
 * <p>质检单号：OI{yyyyMMdd}{seq:4}
 * <p>4 状态：DRAFT/PASSED/FAILED/CONDITIONAL
 * <p>3 P1 修补：单一 163 邮箱 / 检验项目必�?/ 严重度分�? */
@Service
public class OutsourceIncomingInspectionService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CONDITIONAL = "CONDITIONAL";

    public static final String SEVERITY_MINOR = "MINOR";
    public static final String SEVERITY_MAJOR = "MAJOR";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    /** V1.3.7 AD-3：单一 163 邮箱 */
    public static final String VALID_EMAIL_SUFFIX = "@163.com";

    /** 条件接受阈�?0~10% 不良�?*/
    public static final BigDecimal CONDITIONAL_THRESHOLD = new BigDecimal("10.00");
    /** 失败阈�?> 10% 不良�?*/
    public static final BigDecimal FAIL_THRESHOLD = new BigDecimal("10.00");

    private final CrmOutsourceIncomingInspectionMapper inspectionMapper;
    private final CrmOutsourceIncomingItemMapper itemMapper;
    private final CrmOutsourceIncomingDefectMapper defectMapper;
    private final CrmOutsourceOrderMapper orderMapper;
    private final ErpDocNoGenerator docNoGenerator;

    @Autowired
    public OutsourceIncomingInspectionService(CrmOutsourceIncomingInspectionMapper inspectionMapper,
                                               CrmOutsourceIncomingItemMapper itemMapper,
                                               CrmOutsourceIncomingDefectMapper defectMapper,
                                               CrmOutsourceOrderMapper orderMapper,
                                               ErpDocNoGenerator docNoGenerator) {
        this.inspectionMapper = inspectionMapper;
        this.itemMapper = itemMapper;
        this.defectMapper = defectMapper;
        this.orderMapper = orderMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-6.5.1 创建来料质检�?     * P1 修补 1�?63 邮箱校验
     * P1 修补 2：检验项目至�?1 �?     */
    @Transactional
    @AuditLog(module = "outsource_incoming", action = "outsource_incoming.create")
    public Result<CrmOutsourceIncomingInspection> createInspection(IncomingInspectionRequest req, Long operatorUserId) {
        if (req == null || req.getOutsourceId() == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        if (req.getInspectQty() == null || req.getInspectQty() <= 0) {
            return Result.fail(40001, "INSPECT_QTY_REQUIRED");
        }
        // P1 修补 2：检验项目必�?
            if (req.getItems() == null || req.getItems().isEmpty()) {
            return Result.fail(40001, "INSPECT_ITEMS_REQUIRED");
        }
        for (IncomingInspectionRequest.IncomingItemDto item : req.getItems()) {
            if (item.getItemName() == null || item.getItemName().isEmpty()) {
                return Result.fail(40001, "INSPECT_ITEM_NAME_REQUIRED");
            }
        }
        // P1 修补 1：单一 163 邮箱
            if (req.getNotifyEmail() != null && !req.getNotifyEmail().isEmpty()
                && !req.getNotifyEmail().toLowerCase().endsWith(VALID_EMAIL_SUFFIX)) {
            return Result.fail(40001, "NOTIFY_EMAIL_MUST_BE_163");
        }

        CrmOutsourceOrder order = orderMapper.selectById(req.getOutsourceId());
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }

        CrmOutsourceIncomingInspection inspection = new CrmOutsourceIncomingInspection();
        inspection.setInspectionNo(docNoGenerator.nextOutsourceInspectionNo());
        inspection.setOutsourceId(order.getId());
        inspection.setOutsourceNo(order.getOutsourceNo());
        inspection.setSupplierId(order.getSupplierId());
        inspection.setSupplierName(order.getSupplierName());
        inspection.setMaterialCode(order.getMaterialCode());
        inspection.setInspectQty(req.getInspectQty());
        inspection.setPassedQty(0);
        inspection.setFailedQty(0);
        inspection.setDefectRate(BigDecimal.ZERO);
        inspection.setResult(STATUS_DRAFT);
        inspection.setNotifyEmail(req.getNotifyEmail());
        inspection.setRemark(req.getRemark());
        inspection.setCreatedBy(operatorUserId);
        inspection.setCreatedAt(LocalDateTime.now());
        inspection.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.insert(inspection);

        // 写检验项�?
            for (IncomingInspectionRequest.IncomingItemDto itemDto : req.getItems()) {
            CrmOutsourceIncomingItem item = new CrmOutsourceIncomingItem();
            item.setInspectionId(inspection.getId());
            item.setItemName(itemDto.getItemName());
            item.setStandard(itemDto.getStandard());
            item.setMeasuredValue(itemDto.getMeasuredValue());
            item.setPassed(itemDto.getPassed() == null ? 0 : itemDto.getPassed());
            item.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(item);
        }

        return Result.ok(inspection);
    }

    /**
     * P1 修补 3：添加不良项（带严重度分级）
     */
    @Transactional
    @AuditLog(module = "outsource_incoming", action = "outsource_incoming.add_defect")
    public Result<CrmOutsourceIncomingDefect> addDefect(AddDefectRequest req, Long operatorUserId) {
        if (req == null || req.getInspectionId() == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        if (req.getDefectType() == null || req.getDefectType().isEmpty()) {
            return Result.fail(40001, "DEFECT_TYPE_REQUIRED");
        }
        if (req.getSeverity() == null
                || (!SEVERITY_MINOR.equals(req.getSeverity())
                && !SEVERITY_MAJOR.equals(req.getSeverity())
                && !SEVERITY_CRITICAL.equals(req.getSeverity()))) {
            return Result.fail(40001, "DEFECT_SEVERITY_INVALID");
        }
        if (req.getQty() == null || req.getQty() <= 0) {
            return Result.fail(40001, "DEFECT_QTY_REQUIRED");
        }

        CrmOutsourceIncomingInspection inspection = inspectionMapper.selectById(req.getInspectionId());
        if (inspection == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }

        CrmOutsourceIncomingDefect defect = new CrmOutsourceIncomingDefect();
        defect.setInspectionId(req.getInspectionId());
        defect.setItemId(req.getItemId());
        defect.setDefectType(req.getDefectType());
        defect.setSeverity(req.getSeverity());
        defect.setQty(req.getQty());
        defect.setDescription(req.getDescription());
        defect.setCreatedAt(LocalDateTime.now());
        defectMapper.insert(defect);

        // 同步累加 failed_qty
            int oldFailed = inspection.getFailedQty() == null ? 0 : inspection.getFailedQty();
        inspection.setFailedQty(oldFailed + req.getQty());
        inspection.setUpdatedAt(LocalDateTime.now());
        // 重算不良�?
            if (inspection.getInspectQty() != null && inspection.getInspectQty() > 0) {
            int passedQty = inspection.getInspectQty() - inspection.getFailedQty();
            if (passedQty < 0) passedQty = 0;
            inspection.setPassedQty(passedQty);
            BigDecimal rate = new BigDecimal(inspection.getFailedQty())
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(inspection.getInspectQty()), 2, RoundingMode.HALF_UP);
            inspection.setDefectRate(rate);
        }
        inspectionMapper.updateById(inspection);

        return Result.ok(defect);
    }

    /**
     * AC-6.5.2 标记 PASSED
     */
    @Transactional
    @AuditLog(module = "outsource_incoming", action = "outsource_incoming.pass")
    public Result<CrmOutsourceIncomingInspection> pass(Long inspectionId, Long operatorUserId) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        CrmOutsourceIncomingInspection inspection = inspectionMapper.selectById(inspectionId);
        if (inspection == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        if (STATUS_FAILED.equals(inspection.getResult())) {
            return Result.fail(40903, "INSPECTION_ALREADY_FAILED");
        }
        if (STATUS_PASSED.equals(inspection.getResult())) {
            return Result.fail(40903, "INSPECTION_ALREADY_PASSED");
        }

        // �?CRITICAL 不良项不可直�?PASS
            int criticalCount = defectMapper.countCriticalByInspectionId(inspectionId);
        if (criticalCount > 0) {
            return Result.fail(40903, "INSPECTION_HAS_CRITICAL_DEFECT");
        }

        int passedQty = inspection.getInspectQty() - (inspection.getFailedQty() == null ? 0 : inspection.getFailedQty());
        if (passedQty < 0) passedQty = 0;
        inspection.setPassedQty(passedQty);
        inspection.setResult(STATUS_PASSED);
        inspection.setInspectorUserId(operatorUserId);
        inspection.setInspectedAt(LocalDateTime.now());
        inspection.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateById(inspection);

        return Result.ok(inspection);
    }

    /**
     * AC-6.5.3 标记 FAILED �?触发返修
     */
    @Transactional
    @AuditLog(module = "outsource_incoming", action = "outsource_incoming.reject")
    public Result<Map<String, Object>> reject(Long inspectionId, String reason, Long operatorUserId) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        CrmOutsourceIncomingInspection inspection = inspectionMapper.selectById(inspectionId);
        if (inspection == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        if (STATUS_FAILED.equals(inspection.getResult())) {
            return Result.fail(40903, "INSPECTION_ALREADY_FAILED");
        }
        inspection.setResult(STATUS_FAILED);
        inspection.setFailedQty(inspection.getInspectQty());
        inspection.setPassedQty(0);
        inspection.setDefectRate(new BigDecimal("100.00"));
        inspection.setInspectorUserId(operatorUserId);
        inspection.setInspectedAt(LocalDateTime.now());
        inspection.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateById(inspection);

        // AC-6.5.3 联动：失败触发返修（返回信号�?controller�?
            Map<String, Object> result = new HashMap<>();
        result.put("inspection", inspection);
        result.put("reworkTriggered", true);
        result.put("reworkReason", reason != null ? reason : "来料质检不通过");
        result.put("outsourceId", inspection.getOutsourceId());
        result.put("outsourceNo", inspection.getOutsourceNo());
        return Result.ok(result);
    }

    /**
     * AC-6.5.2 三�?PASSED/FAILED/CONDITIONAL
     * CONDITIONAL = 不良率在 0~10%
     */
    @Transactional
    @AuditLog(module = "outsource_incoming", action = "outsource_incoming.conditional")
    public Result<CrmOutsourceIncomingInspection> conditional(Long inspectionId, Long operatorUserId) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        CrmOutsourceIncomingInspection inspection = inspectionMapper.selectById(inspectionId);
        if (inspection == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        // 重算并设�?CONDITIONAL
            if (inspection.getDefectRate() != null && inspection.getDefectRate().compareTo(FAIL_THRESHOLD) > 0) {
            return Result.fail(40903, "DEFECT_RATE_EXCEED_10_NOT_ALLOW_CONDITIONAL");
        }
        inspection.setResult(STATUS_CONDITIONAL);
        inspection.setInspectorUserId(operatorUserId);
        inspection.setInspectedAt(LocalDateTime.now());
        inspection.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateById(inspection);

        return Result.ok(inspection);
    }

    /**
     * 查询（支持按 outsourceId / result 过滤�?     */
    @AuditLog(module = "outsource_incoming", action = "outsource_incoming.list")
    public Result<List<CrmOutsourceIncomingInspection>> list(Long outsourceId, String result) {
        List<CrmOutsourceIncomingInspection> list;
        if (outsourceId != null) {
            list = inspectionMapper.selectByOutsourceId(outsourceId);
        } else if (result != null && !result.isEmpty()) {
            list = inspectionMapper.selectByResult(result);
        } else {
            list = inspectionMapper.selectList(null);
        }
        return Result.ok(list);
    }
}
