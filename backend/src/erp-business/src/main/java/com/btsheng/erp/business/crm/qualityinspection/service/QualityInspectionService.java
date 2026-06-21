package com.btsheng.erp.business.crm.qualityinspection.service;

import com.btsheng.erp.business.crm.qualityinspection.dto.AddInspectionItemRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionCreateRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionFinalizeRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionV1389CreateRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionV1389SubmitRequest;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityConcessionApproval;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspection;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspectionItem;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualitySample;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityConcessionApprovalMapper;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityInspectionItemMapper;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityInspectionMapper;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualitySampleMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.workflowevent.service.WorkflowEventService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.28 · 品质·来料/过程/成品检 Service (FR-7-1)
 *
 * <p>5 业务方法：createInspection / addItem / pass / reject / list
 * <p>检验单号：QI{yyyyMMdd}{seq:4}
 * <p>3 检类型：IQC（来料）/IPQC（过程）/OQC（成品）
 * <p>4 状态：DRAFT/PASSED/FAILED/CONDITIONAL
 * <p>3 P1 修补：抽样规�?AQL / 检验项目必�?/ 严重�?4 级（INFO/WARN/ERROR/CRITICAL�? */
@Service
public class QualityInspectionService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CONDITIONAL = "CONDITIONAL";
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_REWORK = "REWORK";
    public static final String STATUS_SCRAPPED = "SCRAPPED";

    public static final String APPROVAL_PENDING = "PENDING";
    public static final String APPROVAL_APPROVED = "APPROVED";
    public static final String APPROVAL_REJECTED = "REJECTED";

    public static final String ROLE_QUALITY_MANAGER = "QUALITY_MANAGER";
    public static final String ROLE_PRODUCTION_MANAGER = "PRODUCTION_MANAGER";
    public static final String WORKFLOW_CONCESSION = "QUALITY_CONCESSION_FLOW";

    public static final String TYPE_IQC = "IQC";
    public static final String TYPE_IPQC = "IPQC";
    public static final String TYPE_OQC = "OQC";

    public static final String SEVERITY_INFO = "INFO";
    public static final String SEVERITY_WARN = "WARN";
    public static final String SEVERITY_ERROR = "ERROR";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    /** 严重度优先级（从低到高） */
    public static final List<String> SEVERITY_RANK = List.of(
            SEVERITY_INFO, SEVERITY_WARN, SEVERITY_ERROR, SEVERITY_CRITICAL);

    private final CrmQualityInspectionMapper inspectionMapper;
    private final CrmQualityInspectionItemMapper itemMapper;
    private final CrmQualitySampleMapper sampleMapper;
    private final CrmQualityConcessionApprovalMapper concessionApprovalMapper;
    private final QualityInspectionDispositionService dispositionService;
    private final WorkflowEventService workflowEventService;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public QualityInspectionService(CrmQualityInspectionMapper inspectionMapper,
                                    CrmQualityInspectionItemMapper itemMapper,
                                    CrmQualitySampleMapper sampleMapper,
                                    CrmQualityConcessionApprovalMapper concessionApprovalMapper,
                                    QualityInspectionDispositionService dispositionService,
                                    WorkflowEventService workflowEventService,
                                    DocNoGenerator docNoGenerator) {
        this.inspectionMapper = inspectionMapper;
        this.itemMapper = itemMapper;
        this.sampleMapper = sampleMapper;
        this.concessionApprovalMapper = concessionApprovalMapper;
        this.dispositionService = dispositionService;
        this.workflowEventService = workflowEventService;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-7.1.1/7.1.2/7.1.3 创建检单（IQC 来料 / IPQC 过程 / OQC 成品�?     * P1 修补 1：抽样规�?AQL · AQL 等级校验
     * P1 修补 2：检验项目必�?     * P1 修补 3：严重度 4 级（INFO/WARN/ERROR/CRITICAL�?     */
    @Transactional
    @AuditLog(module = "quality_inspection", action = "quality_inspection.create")
    public Result<CrmQualityInspection> createInspection(InspectionCreateRequest req, Long operatorUserId) {
        if (req == null || req.getInspectType() == null) {
            return Result.fail(40001, "INSPECT_TYPE_REQUIRED");
        }
        if (!TYPE_IQC.equals(req.getInspectType())
                && !TYPE_IPQC.equals(req.getInspectType())
                && !TYPE_OQC.equals(req.getInspectType())) {
            return Result.fail(40001, "INSPECT_TYPE_INVALID");
        }
        // 业务校验：IQC 必填物料；IPQC 必填工单+工序；OQC 必填工单
            if (TYPE_IQC.equals(req.getInspectType()) && req.getMaterialId() == null) {
            return Result.fail(40001, "IQC_MATERIAL_REQUIRED");
        }
        if (TYPE_IPQC.equals(req.getInspectType())) {
            if (req.getWorkOrderId() == null
                    && (req.getWorkOrderNo() == null || req.getWorkOrderNo().isEmpty())) {
                return Result.fail(40001, "IPQC_WORKORDER_REQUIRED");
            }
            if (req.getProcessName() == null || req.getProcessName().isEmpty()) {
                req.setProcessName("现场工序");
            }
        }
        if (TYPE_OQC.equals(req.getInspectType()) && req.getWorkOrderId() == null) {
            return Result.fail(40001, "OQC_WORKORDER_REQUIRED");
        }
        // P1 修补 1：AQL 等级校验
            if (req.getAqlLevel() != null && !isValidAqlLevel(req.getAqlLevel())) {
            return Result.fail(40001, "AQL_LEVEL_INVALID");
        }
        // P1 修补 2：检验项目必�?
            if (req.getItems() == null || req.getItems().isEmpty()) {
            return Result.fail(40001, "INSPECTION_ITEMS_REQUIRED");
        }
        // P1 修补 3：严重度 4 �?
            for (InspectionCreateRequest.InspectionItemDto item : req.getItems()) {
            if (item.getItemName() == null || item.getItemName().isEmpty()) {
                return Result.fail(40001, "INSPECTION_ITEM_NAME_REQUIRED");
            }
            if (item.getSeverity() == null || !SEVERITY_RANK.contains(item.getSeverity())) {
                return Result.fail(40001, "INSPECTION_SEVERITY_INVALID");
            }
        }

        CrmQualityInspection insp = new CrmQualityInspection();
        insp.setInspectionNo(docNoGenerator.nextQualityInspectionNo());
        insp.setInspectType(req.getInspectType());
        insp.setMaterialId(req.getMaterialId());
        insp.setMaterialCode(req.getMaterialCode());
        insp.setMaterialName(req.getMaterialName());
        insp.setWorkOrderId(req.getWorkOrderId());
        insp.setWorkOrderNo(req.getWorkOrderNo());
        insp.setProcessName(req.getProcessName());
        insp.setBatchNo(req.getBatchNo());
        insp.setLotSize(req.getLotSize() == null ? 0 : req.getLotSize());
        insp.setSampleSize(req.getSampleSize() == null ? 0 : req.getSampleSize());
        insp.setSampleRule("AQL-" + (req.getAqlLevel() == null ? "1.0" : req.getAqlLevel()));
        insp.setAqlLevel(req.getAqlLevel() == null ? "1.0" : req.getAqlLevel());
        insp.setInspectQty(req.getItems().size());
        insp.setPassedQty(0);
        insp.setFailedQty(0);
        insp.setDefectRate(BigDecimal.ZERO);
        insp.setResult(STATUS_DRAFT);
        insp.setRemark(req.getRemark());
        insp.setCreatedBy(operatorUserId);
        insp.setCreatedAt(LocalDateTime.now());
        insp.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.insert(insp);

        // 写检验项�?
            String maxSeverity = SEVERITY_INFO;
        int failedItems = 0;
        for (InspectionCreateRequest.InspectionItemDto itemDto : req.getItems()) {
            CrmQualityInspectionItem item = new CrmQualityInspectionItem();
            item.setInspectionId(insp.getId());
            item.setItemName(itemDto.getItemName());
            item.setStandard(itemDto.getStandard());
            item.setMeasuredValue(itemDto.getMeasuredValue());
            item.setSeverity(itemDto.getSeverity());
            item.setPassed(itemDto.getPassed() == null ? 0 : itemDto.getPassed());
            item.setDefectDesc(itemDto.getDefectDesc());
            item.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(item);
            // 计算最大严重度
            if (SEVERITY_RANK.indexOf(itemDto.getSeverity()) > SEVERITY_RANK.indexOf(maxSeverity)) {
                maxSeverity = itemDto.getSeverity();
            }
            if (item.getPassed() == 0) failedItems++;
        }
        insp.setMaxSeverity(maxSeverity);
        insp.setFailedQty(failedItems);
        int passed = insp.getInspectQty() - failedItems;
        if (passed < 0) passed = 0;
        insp.setPassedQty(passed);
        if (insp.getInspectQty() > 0) {
            BigDecimal rate = new BigDecimal(failedItems)
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(insp.getInspectQty()), 2, RoundingMode.HALF_UP);
            insp.setDefectRate(rate);
        }
        inspectionMapper.updateById(insp);

        // 写抽�?
            if (req.getSamples() != null) {
            for (InspectionCreateRequest.SampleDto sampleDto : req.getSamples()) {
                CrmQualitySample sample = new CrmQualitySample();
                sample.setInspectionId(insp.getId());
                sample.setSampleNo(sampleDto.getSampleNo());
                sample.setSampleQty(sampleDto.getSampleQty() == null ? 1 : sampleDto.getSampleQty());
                sample.setDefectQty(sampleDto.getDefectQty() == null ? 0 : sampleDto.getDefectQty());
                sample.setAqlPassed(sampleDto.getAqlPassed() == null ? 0 : sampleDto.getAqlPassed());
                sample.setRemark(sampleDto.getRemark());
                sample.setCreatedAt(LocalDateTime.now());
                sampleMapper.insert(sample);
            }
        }

        return Result.ok(insp);
    }

    /**
     * 追加检验项目（带严重度�?     */
    @Transactional
    @AuditLog(module = "quality_inspection", action = "quality_inspection.add_item")
    public Result<CrmQualityInspectionItem> addItem(AddInspectionItemRequest req, Long operatorUserId) {
        if (req == null || req.getInspectionId() == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        if (req.getItemName() == null || req.getItemName().isEmpty()) {
            return Result.fail(40001, "INSPECTION_ITEM_NAME_REQUIRED");
        }
        if (req.getSeverity() == null || !SEVERITY_RANK.contains(req.getSeverity())) {
            return Result.fail(40001, "INSPECTION_SEVERITY_INVALID");
        }
        CrmQualityInspection insp = inspectionMapper.selectById(req.getInspectionId());
        if (insp == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }

        CrmQualityInspectionItem item = new CrmQualityInspectionItem();
        item.setInspectionId(req.getInspectionId());
        item.setItemName(req.getItemName());
        item.setStandard(req.getStandard());
        item.setMeasuredValue(req.getMeasuredValue());
        item.setSeverity(req.getSeverity());
        item.setPassed(req.getPassed() == null ? 0 : req.getPassed());
        item.setDefectDesc(req.getDefectDesc());
        item.setCreatedAt(LocalDateTime.now());
        itemMapper.insert(item);

        // 同步累加
            insp.setInspectQty((insp.getInspectQty() == null ? 0 : insp.getInspectQty()) + 1);
        if (item.getPassed() == 0) {
            insp.setFailedQty((insp.getFailedQty() == null ? 0 : insp.getFailedQty()) + 1);
        } else {
            insp.setPassedQty((insp.getPassedQty() == null ? 0 : insp.getPassedQty()) + 1);
        }
        if (insp.getMaxSeverity() == null
                || SEVERITY_RANK.indexOf(req.getSeverity()) > SEVERITY_RANK.indexOf(insp.getMaxSeverity())) {
            insp.setMaxSeverity(req.getSeverity());
        }
        if (insp.getInspectQty() > 0) {
            BigDecimal rate = new BigDecimal(insp.getFailedQty())
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(insp.getInspectQty()), 2, RoundingMode.HALF_UP);
            insp.setDefectRate(rate);
        }
        insp.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateById(insp);

        return Result.ok(item);
    }

    /**
     * APP 品检提交：更新检验项实测 → pass / reject
     */
    @Transactional
    @AuditLog(module = "quality_inspection", action = "quality_inspection.submit")
    public Result<Map<String, Object>> submitInspection(Long inspectionId,
                                                        InspectionV1389SubmitRequest req,
                                                        Long operatorUserId) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            return Result.fail(40001, "INSPECTION_ITEMS_REQUIRED");
        }
        if (req.getConclusion() == null || req.getConclusion().isBlank()) {
            return Result.fail(40001, "CONCLUSION_REQUIRED");
        }
        CrmQualityInspection insp = inspectionMapper.selectById(inspectionId);
        if (insp == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        if (STATUS_PASSED.equals(insp.getResult()) || STATUS_FAILED.equals(insp.getResult())
                || STATUS_RETURNED.equals(insp.getResult()) || STATUS_REWORK.equals(insp.getResult())
                || STATUS_SCRAPPED.equals(insp.getResult()) || STATUS_CONDITIONAL.equals(insp.getResult())) {
            return Result.fail(40903, "INSPECTION_ALREADY_CLOSED");
        }

        List<CrmQualityInspectionItem> dbItems = itemMapper.selectByInspectionId(inspectionId);
        Map<Long, CrmQualityInspectionItem> byId = new HashMap<>();
        Map<String, CrmQualityInspectionItem> byName = new HashMap<>();
        for (CrmQualityInspectionItem it : dbItems) {
            if (it.getId() != null) {
                byId.put(it.getId(), it);
            }
            if (it.getItemName() != null) {
                byName.put(it.getItemName(), it);
            }
        }

        int failed = 0;
        String maxSeverity = SEVERITY_INFO;
        for (InspectionV1389SubmitRequest.ItemSubmitDto dto : req.getItems()) {
            CrmQualityInspectionItem item = null;
            if (dto.getId() != null) {
                item = byId.get(dto.getId());
            }
            if (item == null && dto.getItemName() != null) {
                item = byName.get(dto.getItemName());
            }
            if (item == null) {
                continue;
            }
            if (dto.getMeasuredValue() != null) {
                item.setMeasuredValue(dto.getMeasuredValue());
            }
            String severity = dto.getSeverity();
            if (severity == null || !SEVERITY_RANK.contains(severity)) {
                severity = SEVERITY_INFO;
            }
            item.setSeverity(severity);
            boolean pass = isPassResult(dto.getResult());
            item.setPassed(pass ? 1 : 0);
            item.setDefectDesc(dto.getDefectDesc());
            itemMapper.updateById(item);
            if (!pass) {
                failed++;
            }
            if (SEVERITY_RANK.indexOf(severity) > SEVERITY_RANK.indexOf(maxSeverity)) {
                maxSeverity = severity;
            }
        }

        int total = insp.getInspectQty() == null ? dbItems.size() : insp.getInspectQty();
        int passed = Math.max(0, total - failed);
        insp.setFailedQty(failed);
        insp.setPassedQty(passed);
        insp.setMaxSeverity(maxSeverity);
        if (total > 0) {
            BigDecimal rate = new BigDecimal(failed)
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
            insp.setDefectRate(rate);
        }
        if (req.getRemark() != null && !req.getRemark().isBlank()) {
            insp.setRemark(req.getRemark());
        }
        insp.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateById(insp);

        if (req.getOverallResult() != null && !req.getOverallResult().isBlank()) {
            InspectionFinalizeRequest fin = new InspectionFinalizeRequest();
            fin.setOverallResult(req.getOverallResult());
            fin.setDisposition(req.getDisposition());
            fin.setDefectQty(req.getDefectQty());
            fin.setConditionalReason(req.getConditionalReason());
            fin.setRemark(req.getRemark());
            return finalizeInspection(inspectionId, fin, operatorUserId);
        }

        boolean wantPass = isPassResult(req.getConclusion()) && failed == 0;
        if (wantPass) {
            return pass(inspectionId, operatorUserId);
        }
        return reject(inspectionId, req.getRejectReason(), operatorUserId);
    }

    /**
     * Web/APP 提交即判定：合格/不合格处置/让步审批
     */
    @Transactional
    @AuditLog(module = "quality_inspection", action = "quality_inspection.finalize")
    public Result<Map<String, Object>> finalizeInspection(Long inspectionId,
                                                           InspectionFinalizeRequest req,
                                                           Long operatorUserId) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        if (req == null || req.getOverallResult() == null || req.getOverallResult().isBlank()) {
            return Result.fail(40001, "OVERALL_RESULT_REQUIRED");
        }
        CrmQualityInspection insp = inspectionMapper.selectById(inspectionId);
        if (insp == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        if (!STATUS_DRAFT.equals(insp.getResult()) && !STATUS_PENDING_APPROVAL.equals(insp.getResult())) {
            return Result.fail(40903, "INSPECTION_ALREADY_CLOSED");
        }

        if (req.getInspectItems() != null && !req.getInspectItems().isEmpty()) {
            replaceItemsFromDto(inspectionId, req.getInspectItems());
            insp = inspectionMapper.selectById(inspectionId);
        }
        if (req.getDrawingNo() != null && !req.getDrawingNo().isBlank()) {
            insp.setDrawingNo(req.getDrawingNo().trim());
        }
        if (req.getRemark() != null && !req.getRemark().isBlank()) {
            insp.setRemark(req.getRemark());
        }

        String overall = req.getOverallResult().trim().toUpperCase();
        Map<String, Object> result = new HashMap<>();
        result.put("inspectionId", inspectionId);
        result.put("inspectionNo", insp.getInspectionNo());

        if ("PASS".equals(overall)) {
            Result<Map<String, Object>> passResult = pass(inspectionId, operatorUserId);
            if (passResult.getCode() != 0) {
                return passResult;
            }
            insp = inspectionMapper.selectById(inspectionId);
            if (TYPE_IQC.equals(insp.getInspectType()) || TYPE_OQC.equals(insp.getInspectType())) {
                insp.setTriggerStockin(1);
                inspectionMapper.updateById(insp);
            }
            result.put("status", STATUS_PASSED);
            result.put("statusLabel", statusLabel(STATUS_PASSED));
            result.put("triggerStockin", insp.getTriggerStockin() == 1);
            return Result.ok(result);
        }

        if ("CONDITIONAL".equals(overall)) {
            if (req.getConditionalReason() == null || req.getConditionalReason().isBlank()) {
                return Result.fail(40001, "CONDITIONAL_REASON_REQUIRED");
            }
            insp.setResult(STATUS_PENDING_APPROVAL);
            insp.setApprovalStatus(APPROVAL_PENDING);
            insp.setRemark(appendRemark(insp.getRemark(), "让步原因：" + req.getConditionalReason()));
            insp.setInspectorUserId(operatorUserId);
            insp.setInspectedAt(LocalDateTime.now());
            insp.setUpdatedAt(LocalDateTime.now());
            inspectionMapper.updateById(insp);
            createConcessionApprovals(inspectionId);
            workflowEventService.recordEvent(WORKFLOW_CONCESSION, inspectionId, insp.getInspectionNo(),
                    WorkflowEventService.EVENT_CREATED, ROLE_QUALITY_MANAGER, null, null,
                    "待品质主管审批", 1, null);
            workflowEventService.recordEvent(WORKFLOW_CONCESSION, inspectionId, insp.getInspectionNo(),
                    WorkflowEventService.EVENT_CREATED, ROLE_PRODUCTION_MANAGER, null, null,
                    "待生管审批", 2, null);
            result.put("status", STATUS_PENDING_APPROVAL);
            result.put("statusLabel", statusLabel(STATUS_PENDING_APPROVAL));
            return Result.ok(result);
        }

        if ("FAIL".equals(overall)) {
            if (req.getDisposition() == null || req.getDisposition().isBlank()) {
                return Result.fail(40001, "DISPOSITION_REQUIRED");
            }
            if (req.getDefectQty() == null || req.getDefectQty() <= 0) {
                return Result.fail(40001, "DEFECT_QTY_REQUIRED");
            }
            String disp = req.getDisposition().trim().toUpperCase();
            insp.setDisposition(disp);
            insp.setDefectDispositionQty(req.getDefectQty());
            insp.setInspectorUserId(operatorUserId);
            insp.setInspectedAt(LocalDateTime.now());
            insp.setFailedQty(req.getDefectQty());
            int lot = insp.getLotSize() == null || insp.getLotSize() <= 0 ? req.getDefectQty() : insp.getLotSize();
            insp.setPassedQty(Math.max(0, lot - req.getDefectQty()));
            insp.setUpdatedAt(LocalDateTime.now());

            Result<Map<String, Object>> downstream;
            switch (disp) {
                case QualityInspectionDispositionService.TYPE_RETURN -> {
                    insp.setResult(STATUS_RETURNED);
                    inspectionMapper.updateById(insp);
                    downstream = dispositionService.createReturnOrder(insp, req.getDefectQty(), operatorUserId);
                }
                case QualityInspectionDispositionService.TYPE_REWORK -> {
                    insp.setResult(STATUS_REWORK);
                    insp.setTriggerRework(1);
                    inspectionMapper.updateById(insp);
                    downstream = dispositionService.createReworkOrder(insp, req.getDefectQty(), operatorUserId);
                }
                case QualityInspectionDispositionService.TYPE_SCRAP -> {
                    insp.setResult(STATUS_SCRAPPED);
                    insp.setPassedQty(0);
                    inspectionMapper.updateById(insp);
                    downstream = dispositionService.createScrapRecord(insp, req.getDefectQty(), operatorUserId);
                }
                default -> {
                    return Result.fail(40001, "DISPOSITION_INVALID");
                }
            }
            if (downstream.getCode() != 0) {
                return Result.fail(downstream.getCode(), downstream.getMessage());
            }
            result.put("status", insp.getResult());
            result.put("statusLabel", statusLabel(insp.getResult()));
            result.put("downstreamOrderNo", downstream.getData().get("orderNo"));
            return Result.ok(result);
        }

        return Result.fail(40001, "OVERALL_RESULT_INVALID");
    }

    @Transactional
    @AuditLog(module = "quality_inspection", action = "quality_inspection.approve_concession")
    public Result<Map<String, Object>> approveConcession(Long inspectionId, String approverRole,
                                                           String action, String comment, Long operatorUserId) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        if (approverRole == null || approverRole.isBlank()) {
            return Result.fail(40001, "APPROVER_ROLE_REQUIRED");
        }
        CrmQualityInspection insp = inspectionMapper.selectById(inspectionId);
        if (insp == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        if (!STATUS_PENDING_APPROVAL.equals(insp.getResult())) {
            return Result.fail(40903, "INSPECTION_NOT_PENDING_APPROVAL");
        }
        CrmQualityConcessionApproval row = concessionApprovalMapper.selectByInspectionAndRole(
                inspectionId, approverRole.trim().toUpperCase());
        if (row == null) {
            return Result.fail(40404, "APPROVAL_TASK_NOT_FOUND");
        }
        if (!APPROVAL_PENDING.equals(row.getApprovalStatus())) {
            return Result.fail(40903, "APPROVAL_ALREADY_DONE");
        }
        boolean approve = "APPROVE".equalsIgnoreCase(action) || "APPROVED".equalsIgnoreCase(action);
        row.setApprovalStatus(approve ? APPROVAL_APPROVED : APPROVAL_REJECTED);
        row.setApproverUserId(operatorUserId);
        row.setApprovedAt(LocalDateTime.now());
        row.setComment(comment);
        concessionApprovalMapper.updateById(row);

        workflowEventService.recordEvent(WORKFLOW_CONCESSION, inspectionId, insp.getInspectionNo(),
                approve ? WorkflowEventService.EVENT_APPROVED : WorkflowEventService.EVENT_REJECTED,
                approverRole.trim().toUpperCase(), operatorUserId, null, comment, null, null);

        Map<String, Object> result = new HashMap<>();
        if (!approve) {
            insp.setResult(STATUS_FAILED);
            insp.setApprovalStatus(APPROVAL_REJECTED);
            insp.setUpdatedAt(LocalDateTime.now());
            inspectionMapper.updateById(insp);
            result.put("status", STATUS_FAILED);
            result.put("statusLabel", "审批驳回");
            return Result.ok(result);
        }

        List<CrmQualityConcessionApproval> all = concessionApprovalMapper.selectByInspectionId(inspectionId);
        boolean allApproved = all.stream().allMatch(a -> APPROVAL_APPROVED.equals(a.getApprovalStatus()));
        if (allApproved) {
            insp.setResult(STATUS_CONDITIONAL);
            insp.setApprovalStatus(APPROVAL_APPROVED);
            insp.setUpdatedAt(LocalDateTime.now());
            inspectionMapper.updateById(insp);
            result.put("status", STATUS_CONDITIONAL);
            result.put("statusLabel", statusLabel(STATUS_CONDITIONAL));
        } else {
            result.put("status", STATUS_PENDING_APPROVAL);
            result.put("statusLabel", statusLabel(STATUS_PENDING_APPROVAL));
        }
        return Result.ok(result);
    }

    public static String statusLabel(String result) {
        if (result == null) {
            return "待检验";
        }
        return switch (result.toUpperCase()) {
            case STATUS_DRAFT -> "待检验";
            case STATUS_PASSED -> "已合格";
            case STATUS_PENDING_APPROVAL -> "待审批";
            case STATUS_CONDITIONAL -> "已让步";
            case STATUS_RETURNED -> "已退货";
            case STATUS_REWORK -> "待返工";
            case STATUS_SCRAPPED -> "已报废";
            case STATUS_FAILED -> "不合格";
            default -> result;
        };
    }

    private void createConcessionApprovals(Long inspectionId) {
        for (String role : List.of(ROLE_QUALITY_MANAGER, ROLE_PRODUCTION_MANAGER)) {
            CrmQualityConcessionApproval row = new CrmQualityConcessionApproval();
            row.setInspectionId(inspectionId);
            row.setApproverRole(role);
            row.setApprovalStatus(APPROVAL_PENDING);
            row.setCreatedAt(LocalDateTime.now());
            concessionApprovalMapper.insert(row);
        }
    }

    private void replaceItemsFromDto(Long inspectionId,
                                     List<InspectionV1389CreateRequest.InspectionV1389ItemDto> items) {
        for (CrmQualityInspectionItem old : itemMapper.selectByInspectionId(inspectionId)) {
            itemMapper.deleteById(old.getId());
        }
        int failed = 0;
        String maxSeverity = SEVERITY_INFO;
        for (InspectionV1389CreateRequest.InspectionV1389ItemDto dto : items) {
            if (dto.getItemName() == null || dto.getItemName().isBlank()) {
                continue;
            }
            CrmQualityInspectionItem item = new CrmQualityInspectionItem();
            item.setInspectionId(inspectionId);
            item.setItemName(dto.getItemName());
            item.setStandard(dto.getStandard());
            item.setMeasuredValue(dto.getMeasuredValue());
            item.setSeverity(SEVERITY_INFO);
            boolean ok = dto.getResult() == null || "OK".equalsIgnoreCase(dto.getResult())
                    || "PASS".equalsIgnoreCase(dto.getResult());
            item.setPassed(ok ? 1 : 0);
            item.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(item);
            if (!ok) {
                failed++;
            }
        }
        CrmQualityInspection insp = inspectionMapper.selectById(inspectionId);
        int total = items.size();
        insp.setInspectQty(total);
        insp.setFailedQty(failed);
        insp.setPassedQty(Math.max(0, total - failed));
        if (total > 0) {
            BigDecimal rate = new BigDecimal(failed)
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
            insp.setDefectRate(rate);
        }
        insp.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateById(insp);
    }

    private static String appendRemark(String existing, String extra) {
        if (existing == null || existing.isBlank()) {
            return extra;
        }
        return existing + " · " + extra;
    }

    private static boolean isPassResult(String result) {
        if (result == null) {
            return false;
        }
        String r = result.trim().toUpperCase();
        return "PASS".equals(r) || "OK".equals(r) || "PASSED".equals(r) || "合格".equals(result.trim());
    }

    /**
     * AC-7.1.4 通过检验（OQC 通过 �?触发入库�?     */
    @Transactional
    @AuditLog(module = "quality_inspection", action = "quality_inspection.pass")
    public Result<Map<String, Object>> pass(Long inspectionId, Long operatorUserId) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        CrmQualityInspection insp = inspectionMapper.selectById(inspectionId);
        if (insp == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        if (STATUS_PASSED.equals(insp.getResult())) {
            return Result.fail(40903, "INSPECTION_ALREADY_PASSED");
        }
        if (STATUS_FAILED.equals(insp.getResult())) {
            return Result.fail(40903, "INSPECTION_ALREADY_FAILED");
        }
        // CRITICAL 严重�?�?不允许通过
            if (SEVERITY_CRITICAL.equals(insp.getMaxSeverity())) {
            return Result.fail(40903, "INSPECTION_HAS_CRITICAL_ITEM");
        }
        int passed = insp.getInspectQty() - (insp.getFailedQty() == null ? 0 : insp.getFailedQty());
        if (passed < 0) passed = 0;
        insp.setPassedQty(passed);
        insp.setResult(STATUS_PASSED);
        insp.setInspectorUserId(operatorUserId);
        insp.setInspectedAt(LocalDateTime.now());
        // OQC 通过 �?触发入库
            if (TYPE_OQC.equals(insp.getInspectType())) {
            insp.setTriggerStockin(1);
        }
        insp.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateById(insp);

        Map<String, Object> result = new HashMap<>();
        result.put("inspection", insp);
        result.put("triggerStockin", insp.getTriggerStockin() == 1);
        result.put("workOrderId", insp.getWorkOrderId());
        return Result.ok(result);
    }

    /**
     * AC-7.1.4 拒绝检验（IQC 不通过 �?触发返修�?     */
    @Transactional
    @AuditLog(module = "quality_inspection", action = "quality_inspection.reject")
    public Result<Map<String, Object>> reject(Long inspectionId, String reason, Long operatorUserId) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        CrmQualityInspection insp = inspectionMapper.selectById(inspectionId);
        if (insp == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        if (STATUS_FAILED.equals(insp.getResult())) {
            return Result.fail(40903, "INSPECTION_ALREADY_FAILED");
        }
        insp.setResult(STATUS_FAILED);
        insp.setFailedQty(insp.getInspectQty());
        insp.setPassedQty(0);
        insp.setDefectRate(new BigDecimal("100.00"));
        insp.setInspectorUserId(operatorUserId);
        insp.setInspectedAt(LocalDateTime.now());
        // IQC 不通过 �?触发返修
            if (TYPE_IQC.equals(insp.getInspectType())) {
            insp.setTriggerRework(1);
        }
        insp.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateById(insp);

        Map<String, Object> result = new HashMap<>();
        result.put("inspection", insp);
        result.put("triggerRework", insp.getTriggerRework() == 1);
        result.put("reworkReason", reason != null ? reason : "来料/过程/成品检不通过");
        result.put("materialId", insp.getMaterialId());
        result.put("workOrderId", insp.getWorkOrderId());
        return Result.ok(result);
    }

    /**
     * 查询列表（按类型/物料/工单/结果过滤�?     */
    @AuditLog(module = "quality_inspection", action = "quality_inspection.list")
    public Result<List<CrmQualityInspection>> list(String inspectType, Long materialId,
                                                    Long workOrderId, String result) {
        List<CrmQualityInspection> list;
        if (inspectType != null && !inspectType.isEmpty()) {
            list = inspectionMapper.selectByInspectType(inspectType);
        } else if (materialId != null) {
            list = inspectionMapper.selectByMaterialId(materialId);
        } else if (workOrderId != null) {
            list = inspectionMapper.selectByWorkOrderId(workOrderId);
        } else if (result != null && !result.isEmpty()) {
            list = inspectionMapper.selectByResult(result);
        } else {
            list = inspectionMapper.selectList(null);
        }
        return Result.ok(list);
    }

    public Result<Map<String, Object>> getDetail(Long inspectionId) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        CrmQualityInspection insp = inspectionMapper.selectById(inspectionId);
        if (insp == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", insp.getId());
        detail.put("inspectionNo", insp.getInspectionNo());
        detail.put("type", insp.getInspectType());
        detail.put("materialCode", insp.getMaterialCode());
        detail.put("materialName", insp.getMaterialName());
        detail.put("workOrderNo", insp.getWorkOrderNo());
        detail.put("processName", insp.getProcessName());
        detail.put("qty", insp.getInspectQty());
        detail.put("passQty", insp.getPassedQty());
        detail.put("failQty", insp.getFailedQty());
        detail.put("inspector", insp.getInspectorUserId());
        detail.put("inspectedAt", insp.getInspectedAt());
        detail.put("result", insp.getResult());
        detail.put("statusLabel", statusLabel(insp.getResult()));
        detail.put("disposition", insp.getDisposition());
        detail.put("approvalStatus", insp.getApprovalStatus());
        detail.put("defectQty", insp.getDefectDispositionQty());
        detail.put("drawingNo", insp.getDrawingNo());
        detail.put("remark", insp.getRemark());
        List<Map<String, Object>> items = new ArrayList<>();
        for (CrmQualityInspectionItem item : itemMapper.selectByInspectionId(inspectionId)) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", item.getId());
            row.put("itemName", item.getItemName());
            row.put("standard", item.getStandard());
            row.put("actual", item.getMeasuredValue());
            row.put("result", item.getPassed() != null && item.getPassed() == 1 ? "PASS" : "FAIL");
            row.put("severity", item.getSeverity());
            row.put("defectDesc", item.getDefectDesc());
            items.add(row);
        }
        detail.put("items", items);
        return Result.ok(detail);
    }

    public Result<Map<String, Object>> getReport(Long inspectionId) {
        Result<Map<String, Object>> detail = getDetail(inspectionId);
        if (!detail.isSuccess()) {
            return detail;
        }
        Map<String, Object> report = new HashMap<>(detail.getData());
        report.put("reportTitle", "品质检验报告");
        report.put("generatedAt", LocalDateTime.now());
        Object result = report.get("result");
        report.put("conclusion", STATUS_PASSED.equals(result) ? "合格" : STATUS_FAILED.equals(result) ? "不合格" : "检验中");
        return Result.ok(report);
    }

    public Result<List<Map<String, Object>>> getConcessionApprovals(Long inspectionId) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        CrmQualityInspection insp = inspectionMapper.selectById(inspectionId);
        if (insp == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CrmQualityConcessionApproval a : concessionApprovalMapper.selectByInspectionId(inspectionId)) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", a.getId());
            row.put("approverRole", a.getApproverRole());
            row.put("approverRoleLabel", concessionRoleLabel(a.getApproverRole()));
            row.put("approvalStatus", a.getApprovalStatus());
            row.put("approverUserId", a.getApproverUserId());
            row.put("approvedAt", a.getApprovedAt());
            row.put("comment", a.getComment());
            rows.add(row);
        }
        return Result.ok(rows);
    }

    private static String concessionRoleLabel(String role) {
        if (role == null) return "";
        return switch (role) {
            case ROLE_QUALITY_MANAGER -> "品质主管";
            case ROLE_PRODUCTION_MANAGER -> "生管";
            default -> role;
        };
    }

    private boolean isValidAqlLevel(String level) {
        return "0.65".equals(level) || "1.0".equals(level)
                || "1.5".equals(level) || "2.5".equals(level) || "4.0".equals(level);
    }
}
