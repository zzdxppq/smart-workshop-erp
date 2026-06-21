package com.btsheng.erp.business.crm.qualityfa.service;

import com.btsheng.erp.business.crm.qualityfa.dto.FaCreateRequest;
import com.btsheng.erp.business.crm.qualityfa.entity.CrmQualityFa;
import com.btsheng.erp.business.crm.qualityfa.entity.CrmQualityFaItem;
import com.btsheng.erp.business.crm.qualityfa.mapper.CrmQualityFaItemMapper;
import com.btsheng.erp.business.crm.qualityfa.mapper.CrmQualityFaMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * V1.3.7 · Story 1.29 · 品质·FA 首件 Service (FR-7-2)
 *
 * <p>4 业务方法：createFa / pass / reject / list
 * <p>FA 单号：QF{yyyyMMdd}{seq:4}
 * <p>3 状态：DRAFT/PASSED/FAILED
 * <p>3 P1 修补：FA 必检（开工前�? 检验项�?8 维度 / 不合格阻断生�?
 *
 * <p>V2.1 品质专项增强：双签流程
 * <p>6 状态：PENDING_INSPECT/INSPECTING/PENDING_SIGN/PASSED/FAILED/REWORK
 * <p>双签：品检员签字 + 工程师签字
 */
@Service
public class QualityFaService {

    /** V2.1 状态常量 */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING_INSPECT = "PENDING_INSPECT";
    public static final String STATUS_INSPECTING = "INSPECTING";
    public static final String STATUS_PENDING_SIGN = "PENDING_SIGN";
    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_REWORK = "REWORK";

    /** P1 修补 2�? 维度 */
    public static final Set<String> DIMENSIONS = Set.of(
            "尺寸", "形位", "粗糙度", "硬度", "材质", "外观", "装配", "性能");

    private final CrmQualityFaMapper faMapper;
    private final CrmQualityFaItemMapper itemMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public QualityFaService(CrmQualityFaMapper faMapper,
                            CrmQualityFaItemMapper itemMapper,
                            DocNoGenerator docNoGenerator) {
        this.faMapper = faMapper;
        this.itemMapper = itemMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-7.2.1 FA 必检（开工前�?     * P1 修补 1：FA 必检（开工前�?     * P1 修补 2：检验项�?8 维度
     */
    @Transactional
    @AuditLog(module = "quality_fa", action = "quality_fa.create")
    public Result<CrmQualityFa> createFa(FaCreateRequest req, Long operatorUserId) {
        if (req == null || req.getWorkOrderId() == null) {
            return Result.fail(40001, "WORK_ORDER_ID_REQUIRED");
        }
        if (req.getProcessId() == null) {
            return Result.fail(40001, "PROCESS_ID_REQUIRED");
        }
        if (req.getProcessName() == null || req.getProcessName().isEmpty()) {
            return Result.fail(40001, "PROCESS_NAME_REQUIRED");
        }
        // P1 修补 2�? 维度校验
            if (req.getItems() == null || req.getItems().isEmpty()) {
            return Result.fail(40001, "FA_ITEMS_REQUIRED");
        }
        for (FaCreateRequest.FaItemDto item : req.getItems()) {
            if (item.getItemName() == null || item.getItemName().isEmpty()) {
                return Result.fail(40001, "FA_ITEM_NAME_REQUIRED");
            }
            if (item.getDimension() == null || !DIMENSIONS.contains(item.getDimension())) {
                return Result.fail(40001, "FA_DIMENSION_INVALID");
            }
        }

        CrmQualityFa fa = new CrmQualityFa();
        fa.setFaNo(docNoGenerator.nextQualityFaNo());
        fa.setWorkOrderId(req.getWorkOrderId());
        fa.setWorkOrderNo(req.getWorkOrderNo());
        fa.setProcessId(req.getProcessId());
        fa.setProcessName(req.getProcessName());
        fa.setOperatorUserId(req.getOperatorUserId());
        fa.setInspectQty(req.getInspectQty() == null ? 1 : req.getInspectQty());
        fa.setResult(STATUS_DRAFT);
        fa.setLocked(0);
        fa.setPdfUrl(req.getPdfUrl());
        fa.setRemark(req.getRemark());
        fa.setCreatedBy(operatorUserId);
        fa.setCreatedAt(LocalDateTime.now());
        fa.setUpdatedAt(LocalDateTime.now());
        faMapper.insert(fa);

        for (FaCreateRequest.FaItemDto itemDto : req.getItems()) {
            CrmQualityFaItem item = new CrmQualityFaItem();
            item.setFaId(fa.getId());
            item.setDimension(itemDto.getDimension());
            item.setItemName(itemDto.getItemName());
            item.setStandard(itemDto.getStandard());
            item.setMeasuredValue(itemDto.getMeasuredValue());
            item.setTolerance(itemDto.getTolerance());
            item.setPassed(itemDto.getPassed() == null ? 0 : itemDto.getPassed());
            item.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(item);
        }
        return Result.ok(fa);
    }

    /**
     * AC-7.2.2 PASSED（生�?PDF 报告�?     */
    @Transactional
    @AuditLog(module = "quality_fa", action = "quality_fa.pass")
    public Result<CrmQualityFa> pass(Long faId, Long operatorUserId) {
        if (faId == null) {
            return Result.fail(40001, "FA_ID_REQUIRED");
        }
        CrmQualityFa fa = faMapper.selectById(faId);
        if (fa == null) {
            return Result.fail(40404, "FA_NOT_FOUND");
        }
        if (STATUS_PASSED.equals(fa.getResult())) {
            return Result.fail(40903, "FA_ALREADY_PASSED");
        }
        if (STATUS_FAILED.equals(fa.getResult())) {
            return Result.fail(40903, "FA_ALREADY_FAILED");
        }
        // 校验所有项目都 PASSED
            List<CrmQualityFaItem> items = itemMapper.selectByFaId(faId);
        for (CrmQualityFaItem item : items) {
            if (item.getPassed() == null || item.getPassed() == 0) {
                return Result.fail(40903, "FA_HAS_FAILED_ITEM");
            }
        }
        fa.setResult(STATUS_PASSED);
        fa.setLocked(0);
        fa.setInspectorUserId(operatorUserId);
        fa.setInspectedAt(LocalDateTime.now());
        // 自动生成 PDF 报告路径
            if (fa.getPdfUrl() == null || fa.getPdfUrl().isEmpty()) {
            fa.setPdfUrl("/reports/fa/" + fa.getFaNo() + ".pdf");
        }
        fa.setUpdatedAt(LocalDateTime.now());
        faMapper.updateById(fa);
        return Result.ok(fa);
    }

    /**
     * AC-7.2.3 FAILED �?锁定工序（不合格阻断生产�?     */
    @Transactional
    @AuditLog(module = "quality_fa", action = "quality_fa.reject")
    public Result<Map<String, Object>> reject(Long faId, String reason, Long operatorUserId) {
        if (faId == null) {
            return Result.fail(40001, "FA_ID_REQUIRED");
        }
        CrmQualityFa fa = faMapper.selectById(faId);
        if (fa == null) {
            return Result.fail(40404, "FA_NOT_FOUND");
        }
        if (STATUS_FAILED.equals(fa.getResult())) {
            return Result.fail(40903, "FA_ALREADY_FAILED");
        }
        fa.setResult(STATUS_FAILED);
        fa.setLocked(1);
        fa.setInspectorUserId(operatorUserId);
        fa.setInspectedAt(LocalDateTime.now());
        fa.setUpdatedAt(LocalDateTime.now());
        faMapper.updateById(fa);

        Map<String, Object> result = new HashMap<>();
        result.put("fa", fa);
        result.put("processLocked", true);
        result.put("workOrderId", fa.getWorkOrderId());
        result.put("processId", fa.getProcessId());
        result.put("lockReason", reason != null ? reason : "FA 首件不合格，自动锁定工序");
        return Result.ok(result);
    }

    @AuditLog(module = "quality_fa", action = "quality_fa.list")
    public Result<List<CrmQualityFa>> list(Long workOrderId, Long processId, String result) {
        List<CrmQualityFa> list;
        if (workOrderId != null) {
            list = faMapper.selectByWorkOrderId(workOrderId);
        } else if (processId != null) {
            list = faMapper.selectByProcessId(processId);
        } else if (result != null && !result.isEmpty()) {
            list = faMapper.selectByResult(result);
        } else {
            list = faMapper.selectList(null);
        }
        return Result.ok(list);
    }

    /**
     * V2.1 增强：品检员签字
     * 状态流转：PENDING_INSPECT -> INSPECTING -> PENDING_SIGN
     */
    @Transactional
    @AuditLog(module = "quality_fa", action = "quality_fa.inspector_sign")
    public Result<CrmQualityFa> inspectorSign(Long faId, Long operatorUserId) {
        if (faId == null) {
            return Result.fail(40001, "FA_ID_REQUIRED");
        }
        CrmQualityFa fa = faMapper.selectById(faId);
        if (fa == null) {
            return Result.fail(40404, "FA_NOT_FOUND");
        }
        if (!STATUS_PENDING_INSPECT.equals(fa.getStatus()) && !STATUS_INSPECTING.equals(fa.getStatus())) {
            return Result.fail(40903, "FA_STATUS_NOT_INSPECTABLE");
        }
        fa.setStatus(STATUS_PENDING_SIGN);
        fa.setInspectorUserId(operatorUserId);
        fa.setInspectorSignedAt(LocalDateTime.now());
        fa.setUpdatedAt(LocalDateTime.now());
        faMapper.updateById(fa);
        return Result.ok(fa);
    }

    /**
     * V2.1 增强：工程师签字（终签）
     * 状态流转：PENDING_SIGN -> PASSED（所有项目通过）/ FAILED（有项目不通过）
     */
    @Transactional
    @AuditLog(module = "quality_fa", action = "quality_fa.engineer_sign")
    public Result<CrmQualityFa> engineerSign(Long faId, Long operatorUserId) {
        if (faId == null) {
            return Result.fail(40001, "FA_ID_REQUIRED");
        }
        CrmQualityFa fa = faMapper.selectById(faId);
        if (fa == null) {
            return Result.fail(40404, "FA_NOT_FOUND");
        }
        if (!STATUS_PENDING_SIGN.equals(fa.getStatus())) {
            return Result.fail(40903, "FA_STATUS_NOT_PENDING_SIGN");
        }
        // 校验所有检验项目是否通过
        List<CrmQualityFaItem> items = itemMapper.selectByFaId(faId);
        boolean allPassed = items.stream().allMatch(item -> item.getPassed() != null && item.getPassed() == 1);
        if (allPassed) {
            fa.setResult(STATUS_PASSED);
            fa.setStatus(STATUS_PASSED);
            fa.setLocked(0);
        } else {
            fa.setResult(STATUS_FAILED);
            fa.setStatus(STATUS_FAILED);
            fa.setLocked(1);
        }
        fa.setEngineerUserId(operatorUserId);
        fa.setEngineerSignedAt(LocalDateTime.now());
        if (fa.getPdfUrl() == null || fa.getPdfUrl().isEmpty()) {
            fa.setPdfUrl("/reports/fa/" + fa.getFaNo() + ".pdf");
        }
        fa.setUpdatedAt(LocalDateTime.now());
        faMapper.updateById(fa);
        return Result.ok(fa);
    }

    /**
     * V2.1 增强：驳回并要求返工
     * 状态流转：PENDING_SIGN -> REWORK -> PENDING_INSPECT（重新检验）
     */
    @Transactional
    @AuditLog(module = "quality_fa", action = "quality_fa.rework")
    public Result<CrmQualityFa> rework(Long faId, String reason, Long operatorUserId) {
        if (faId == null) {
            return Result.fail(40001, "FA_ID_REQUIRED");
        }
        CrmQualityFa fa = faMapper.selectById(faId);
        if (fa == null) {
            return Result.fail(40404, "FA_NOT_FOUND");
        }
        if (!STATUS_PENDING_SIGN.equals(fa.getStatus())) {
            return Result.fail(40903, "FA_STATUS_NOT_REWORKABLE");
        }
        fa.setResult(STATUS_FAILED);
        fa.setStatus(STATUS_REWORK);
        fa.setRejectReason(reason);
        fa.setReworkCount(fa.getReworkCount() == null ? 1 : fa.getReworkCount() + 1);
        fa.setLocked(1);
        fa.setEngineerUserId(operatorUserId);
        fa.setEngineerSignedAt(LocalDateTime.now());
        fa.setUpdatedAt(LocalDateTime.now());
        faMapper.updateById(fa);
        return Result.ok(fa);
    }

    /**
     * V2.1 增强：重新提交检验（返工后）
     * 状态流转：REWORK -> PENDING_INSPECT
     */
    @Transactional
    @AuditLog(module = "quality_fa", action = "quality_fa.resubmit")
    public Result<CrmQualityFa> resubmit(Long faId, Long operatorUserId) {
        if (faId == null) {
            return Result.fail(40001, "FA_ID_REQUIRED");
        }
        CrmQualityFa fa = faMapper.selectById(faId);
        if (fa == null) {
            return Result.fail(40404, "FA_NOT_FOUND");
        }
        if (!STATUS_REWORK.equals(fa.getStatus())) {
            return Result.fail(40903, "FA_STATUS_NOT_RESUBMITTABLE");
        }
        fa.setStatus(STATUS_PENDING_INSPECT);
        fa.setLocked(0);
        fa.setUpdatedAt(LocalDateTime.now());
        faMapper.updateById(fa);
        return Result.ok(fa);
    }
}
