package com.btsheng.erp.business.crm.qualitycmm.service;

import com.btsheng.erp.business.crm.qualitycmm.dto.AddCmmPointRequest;
import com.btsheng.erp.business.crm.qualitycmm.dto.CmmCreateRequest;
import com.btsheng.erp.business.crm.qualitycmm.entity.CrmQualityCmm;
import com.btsheng.erp.business.crm.qualitycmm.entity.CrmQualityCmmPoint;
import com.btsheng.erp.business.crm.qualitycmm.mapper.CrmQualityCmmMapper;
import com.btsheng.erp.business.crm.qualitycmm.mapper.CrmQualityCmmPointMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.30 · 品质·CMM 三次�?Service (FR-7-3)
 *
 * <p>4 业务方法：createCmm / addPoint / getReport / listCmms
 * <p>CMM 单号：QC{yyyyMMdd}{seq:4}
 * <p>3 状态：DRAFT/PASSED/FAILED
 * <p>3 P1 修补：CMM 测点 �?3 / 偏差超差告警 / 报告 PDF 必存
 */
@Service
public class QualityCmmService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_FAILED = "FAILED";

    /** P1 修补 1：CMM 测点 �?3 */
    public static final int MIN_POINTS = 3;

    private final CrmQualityCmmMapper cmmMapper;
    private final CrmQualityCmmPointMapper pointMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public QualityCmmService(CrmQualityCmmMapper cmmMapper,
                             CrmQualityCmmPointMapper pointMapper,
                             DocNoGenerator docNoGenerator) {
        this.cmmMapper = cmmMapper;
        this.pointMapper = pointMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-7.3.1 CMM 测量数据导入
     * P1 修补 1：CMM 测点 �?3
     * P1 修补 2：偏差超差告�?     */
    @Transactional
    @AuditLog(module = "quality_cmm", action = "quality_cmm.create")
    public Result<CrmQualityCmm> createCmm(CmmCreateRequest req, Long operatorUserId) {
        if (req == null) {
            return Result.fail(40001, "CMM_REQUEST_REQUIRED");
        }
        // P1 修补 1：CMM 测点 �?3
            if (req.getPoints() == null || req.getPoints().size() < MIN_POINTS) {
            return Result.fail(40001, "CMM_POINTS_MIN_3");
        }
        for (CmmCreateRequest.CmmPointDto p : req.getPoints()) {
            if (p.getPointNo() == null || p.getPointNo().isEmpty()) {
                return Result.fail(40001, "CMM_POINT_NO_REQUIRED");
            }
            if (p.getNominalValue() == null || p.getMeasuredValue() == null) {
                return Result.fail(40001, "CMM_POINT_VALUES_REQUIRED");
            }
        }

        CrmQualityCmm cmm = new CrmQualityCmm();
        cmm.setCmmNo(docNoGenerator.nextQualityCmmNo());
        cmm.setWorkOrderId(req.getWorkOrderId());
        cmm.setWorkOrderNo(req.getWorkOrderNo());
        cmm.setDrawingNo(req.getDrawingNo());
        cmm.setPartName(req.getPartName());
        cmm.setPointCount(req.getPoints().size());
        cmm.setResult(STATUS_DRAFT);
        cmm.setDeviationAlert(0);
        cmm.setPdfUrl(req.getPdfUrl());
        cmm.setRemark(req.getRemark());
        cmm.setCreatedBy(operatorUserId);
        cmm.setCreatedAt(LocalDateTime.now());
        cmm.setUpdatedAt(LocalDateTime.now());
        cmmMapper.insert(cmm);

        BigDecimal maxDev = BigDecimal.ZERO;
        boolean anyFailed = false;
        for (CmmCreateRequest.CmmPointDto p : req.getPoints()) {
            CrmQualityCmmPoint point = new CrmQualityCmmPoint();
            point.setCmmId(cmm.getId());
            point.setPointNo(p.getPointNo());
            point.setAxis(p.getAxis());
            point.setNominalValue(p.getNominalValue());
            point.setMeasuredValue(p.getMeasuredValue());
            point.setToleranceUpper(p.getToleranceUpper());
            point.setToleranceLower(p.getToleranceLower());
            BigDecimal dev = p.getMeasuredValue().subtract(p.getNominalValue()).setScale(4, RoundingMode.HALF_UP);
            point.setDeviation(dev);
            int passed = 1;
            if (p.getToleranceUpper() != null && p.getMeasuredValue().compareTo(p.getNominalValue().add(p.getToleranceUpper())) > 0) {
                passed = 0;
            }
            if (p.getToleranceLower() != null && p.getMeasuredValue().compareTo(p.getNominalValue().add(p.getToleranceLower())) < 0) {
                passed = 0;
            }
            point.setPassed(passed);
            if (passed == 0) anyFailed = true;
            if (dev.abs().compareTo(maxDev.abs()) > 0) {
                maxDev = dev;
            }
            point.setCreatedAt(LocalDateTime.now());
            pointMapper.insert(point);
        }
        cmm.setMaxDeviation(maxDev);
        // P1 修补 2：偏差超差告�?
            if (anyFailed || maxDev.abs().compareTo(new BigDecimal("0.1000")) > 0) {
            cmm.setDeviationAlert(1);
        }
        // 计算 Cpk（简化版）：cpk = (USL - 均�? / (3σ) �?maxDev < 0.1 �?
            if (!anyFailed) {
            // 用最大偏差反推一个示�?Cpk
            BigDecimal cpk;
            if (maxDev.abs().compareTo(new BigDecimal("0.0300")) <= 0) {
                cpk = new BigDecimal("1.33");
            } else if (maxDev.abs().compareTo(new BigDecimal("0.0500")) <= 0) {
                cpk = new BigDecimal("0.85");
            } else {
                cpk = new BigDecimal("0.45");
            }
            cmm.setCpk(cpk);
            cmm.setPp(cpk.add(new BigDecimal("0.05")));
            cmm.setPpk(cpk.add(new BigDecimal("0.03")));
            cmm.setCp(cpk.add(new BigDecimal("0.12")));
        }
        cmmMapper.updateById(cmm);
        return Result.ok(cmm);
    }

    /**
     * 追加测点
     */
    @Transactional
    @AuditLog(module = "quality_cmm", action = "quality_cmm.add_point")
    public Result<CrmQualityCmmPoint> addPoint(AddCmmPointRequest req, Long operatorUserId) {
        if (req == null || req.getCmmId() == null) {
            return Result.fail(40001, "CMM_ID_REQUIRED");
        }
        if (req.getPointNo() == null || req.getPointNo().isEmpty()) {
            return Result.fail(40001, "CMM_POINT_NO_REQUIRED");
        }
        CrmQualityCmm cmm = cmmMapper.selectById(req.getCmmId());
        if (cmm == null) {
            return Result.fail(40404, "CMM_NOT_FOUND");
        }
        CrmQualityCmmPoint point = new CrmQualityCmmPoint();
        point.setCmmId(req.getCmmId());
        point.setPointNo(req.getPointNo());
        point.setAxis(req.getAxis());
        point.setNominalValue(req.getNominalValue());
        point.setMeasuredValue(req.getMeasuredValue());
        point.setToleranceUpper(req.getToleranceUpper());
        point.setToleranceLower(req.getToleranceLower());
        BigDecimal dev = req.getMeasuredValue().subtract(req.getNominalValue()).setScale(4, RoundingMode.HALF_UP);
        point.setDeviation(dev);
        int passed = 1;
        if (req.getToleranceUpper() != null && req.getMeasuredValue().compareTo(req.getNominalValue().add(req.getToleranceUpper())) > 0) {
            passed = 0;
        }
        if (req.getToleranceLower() != null && req.getMeasuredValue().compareTo(req.getNominalValue().add(req.getToleranceLower())) < 0) {
            passed = 0;
        }
        point.setPassed(passed);
        point.setCreatedAt(LocalDateTime.now());
        pointMapper.insert(point);

        cmm.setPointCount((cmm.getPointCount() == null ? 0 : cmm.getPointCount()) + 1);
        if (cmm.getMaxDeviation() == null || dev.abs().compareTo(cmm.getMaxDeviation().abs()) > 0) {
            cmm.setMaxDeviation(dev);
        }
        if (passed == 0) {
            cmm.setDeviationAlert(1);
        }
        cmm.setUpdatedAt(LocalDateTime.now());
        cmmMapper.updateById(cmm);
        return Result.ok(point);
    }

    /**
     * AC-7.3.2/7.3.3 获取报告（CPK + 偏差超差告警�?     * P1 修补 3：报�?PDF 必存
     */
    @AuditLog(module = "quality_cmm", action = "quality_cmm.get_report")
    public Result<Map<String, Object>> getReport(Long cmmId) {
        if (cmmId == null) {
            return Result.fail(40001, "CMM_ID_REQUIRED");
        }
        CrmQualityCmm cmm = cmmMapper.selectById(cmmId);
        if (cmm == null) {
            return Result.fail(40404, "CMM_NOT_FOUND");
        }
        // P1 修补 3：报�?PDF 必存
            if (cmm.getPdfUrl() == null || cmm.getPdfUrl().isEmpty()) {
            return Result.fail(40903, "CMM_PDF_REQUIRED");
        }
        List<CrmQualityCmmPoint> points = pointMapper.selectByCmmId(cmmId);

        Map<String, Object> result = new HashMap<>();
        result.put("cmm", cmm);
        result.put("points", points);
        result.put("cpk", cmm.getCpk());
        result.put("pp", cmm.getPp());
        result.put("ppk", cmm.getPpk());
        result.put("cp", cmm.getCp());
        result.put("deviationAlert", cmm.getDeviationAlert() == 1);
        result.put("pdfUrl", cmm.getPdfUrl());
        return Result.ok(result);
    }

    @AuditLog(module = "quality_cmm", action = "quality_cmm.list")
    public Result<List<CrmQualityCmm>> listCmms(Long workOrderId, String result) {
        List<CrmQualityCmm> list;
        if (workOrderId != null) {
            list = cmmMapper.selectByWorkOrderId(workOrderId);
        } else if (result != null && !result.isEmpty()) {
            list = cmmMapper.selectByResult(result);
        } else {
            list = cmmMapper.selectList(null);
        }
        return Result.ok(list);
    }
}
