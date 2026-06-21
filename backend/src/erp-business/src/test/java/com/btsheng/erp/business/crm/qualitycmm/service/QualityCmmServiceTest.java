package com.btsheng.erp.business.crm.qualitycmm.service;

import com.btsheng.erp.business.crm.qualitycmm.dto.AddCmmPointRequest;
import com.btsheng.erp.business.crm.qualitycmm.dto.CmmCreateRequest;
import com.btsheng.erp.business.crm.qualitycmm.entity.CrmQualityCmm;
import com.btsheng.erp.business.crm.qualitycmm.entity.CrmQualityCmmPoint;
import com.btsheng.erp.business.crm.qualitycmm.mapper.CrmQualityCmmMapper;
import com.btsheng.erp.business.crm.qualitycmm.mapper.CrmQualityCmmPointMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.30 · 品质·CMM 三次元 Service 单元测试（FR-7-3）
 * 14 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QualityCmmServiceTest {

    @Mock private CrmQualityCmmMapper cmmMapper;
    @Mock private CrmQualityCmmPointMapper pointMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private QualityCmmService service;

    @BeforeEach
    void setUp() {
        service = new QualityCmmService(cmmMapper, pointMapper, docNoGenerator);
        when(docNoGenerator.nextQualityCmmNo())
                .thenReturn("QC20260612-0001", "QC20260612-0002", "QC20260612-0003", "QC20260612-0004");
        when(cmmMapper.insert(any(CrmQualityCmm.class))).thenAnswer(inv -> {
            CrmQualityCmm c = inv.getArgument(0);
            c.setId(1L);
            return 1;
        });
        when(pointMapper.insert(any(CrmQualityCmmPoint.class))).thenAnswer(inv -> {
            CrmQualityCmmPoint p = inv.getArgument(0);
            p.setId(1L);
            return 1;
        });
        when(cmmMapper.updateById(any(CrmQualityCmm.class))).thenReturn(1);
    }

    private CmmCreateRequest.CmmPointDto point(String no, String axis, String nominal, String measured) {
        CmmCreateRequest.CmmPointDto p = new CmmCreateRequest.CmmPointDto();
        p.setPointNo(no);
        p.setAxis(axis);
        p.setNominalValue(new BigDecimal(nominal));
        p.setMeasuredValue(new BigDecimal(measured));
        p.setToleranceUpper(new BigDecimal("0.0500"));
        p.setToleranceLower(new BigDecimal("-0.0500"));
        return p;
    }

    private CmmCreateRequest buildValidReq() {
        CmmCreateRequest req = new CmmCreateRequest();
        req.setWorkOrderId(1L);
        req.setWorkOrderNo("GD20260608-0001");
        req.setDrawingNo("DWG-001");
        req.setPartName("法兰盘");
        req.setPdfUrl("/reports/cmm/test.pdf");
        req.setPoints(Arrays.asList(
                point("P1", "X", "50.0000", "50.0050"),
                point("P2", "Y", "30.0000", "30.0020"),
                point("P3", "Z", "10.0000", "10.0010")));
        return req;
    }

    // ====== createCmm 6 测例 ======
            @Test
    @DisplayName("createCmm happy path · 3 测点 PASSED")
    void testCreate_OK() {
        Result<CrmQualityCmm> r = service.createCmm(buildValidReq(), 401L);
        assertEquals(0, r.getCode());
        assertEquals("QC20260612-0001", r.getData().getCmmNo());
        assertEquals(3, r.getData().getPointCount());
    }

    @Test
    @DisplayName("P1 修补 1：测点 < 3 → 40001")
    void testCreate_PointsMin3() {
        CmmCreateRequest req = buildValidReq();
        req.setPoints(Arrays.asList(
                point("P1", "X", "50.0000", "50.0050"),
                point("P2", "Y", "30.0000", "30.0020")));
        Result<CrmQualityCmm> r = service.createCmm(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("CMM_POINTS_MIN_3", r.getMessage());
    }

    @Test
    @DisplayName("P1 修补 1：测点 = 3 接受（边界）")
    void testCreate_PointsExactly3() {
        Result<CrmQualityCmm> r = service.createCmm(buildValidReq(), 401L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("createCmm 测点编号必填")
    void testCreate_PointNoRequired() {
        CmmCreateRequest req = buildValidReq();
        req.setPoints(Arrays.asList(
                point("", "X", "50.0000", "50.0050"),
                point("P2", "Y", "30.0000", "30.0020"),
                point("P3", "Z", "10.0000", "10.0010")));
        Result<CrmQualityCmm> r = service.createCmm(req, 401L);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("P1 修补 2：偏差超差告警")
    void testCreate_DeviationAlert() {
        CmmCreateRequest req = buildValidReq();
        req.setPoints(Arrays.asList(
                point("P1", "X", "50.0000", "50.1000"),
                point("P2", "Y", "30.0000", "30.0020"),
                point("P3", "Z", "10.0000", "10.0010")));
        Result<CrmQualityCmm> r = service.createCmm(req, 401L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().getDeviationAlert());
    }

    @Test
    @DisplayName("createCmm 缺测点值 → 40001")
    void testCreate_PointValuesRequired() {
        CmmCreateRequest req = buildValidReq();
        CmmCreateRequest.CmmPointDto bad = new CmmCreateRequest.CmmPointDto();
        bad.setPointNo("P1");
        bad.setAxis("X");
        bad.setNominalValue(new BigDecimal("50.0000"));
        bad.setMeasuredValue(null);
        req.setPoints(Arrays.asList(
                bad,
                point("P2", "Y", "30.0000", "30.0020"),
                point("P3", "Z", "10.0000", "10.0010")));
        Result<CrmQualityCmm> r = service.createCmm(req, 401L);
        assertEquals(40001, r.getCode());
    }

    // ====== addPoint 3 测例 ======
            @Test
    @DisplayName("addPoint 追加测点")
    void testAddPoint_OK() {
        CrmQualityCmm cmm = new CrmQualityCmm();
        cmm.setId(1L);
        cmm.setPointCount(3);
        cmm.setMaxDeviation(new BigDecimal("0.0050"));
        when(cmmMapper.selectById(1L)).thenReturn(cmm);

        AddCmmPointRequest req = new AddCmmPointRequest();
        req.setCmmId(1L);
        req.setPointNo("P4");
        req.setAxis("Y");
        req.setNominalValue(new BigDecimal("30.0000"));
        req.setMeasuredValue(new BigDecimal("30.0010"));
        req.setToleranceUpper(new BigDecimal("0.0500"));
        req.setToleranceLower(new BigDecimal("-0.0500"));
        Result<CrmQualityCmmPoint> r = service.addPoint(req, 401L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("addPoint CMM 不存在 → 40404")
    void testAddPoint_NotFound() {
        when(cmmMapper.selectById(99L)).thenReturn(null);
        AddCmmPointRequest req = new AddCmmPointRequest();
        req.setCmmId(99L);
        req.setPointNo("P4");
        Result<CrmQualityCmmPoint> r = service.addPoint(req, 401L);
        assertEquals(40404, r.getCode());
    }

    @Test
    @DisplayName("addPoint 测点编号必填")
    void testAddPoint_NoRequired() {
        AddCmmPointRequest req = new AddCmmPointRequest();
        req.setCmmId(1L);
        req.setPointNo("");
        Result<CrmQualityCmmPoint> r = service.addPoint(req, 401L);
        assertEquals(40001, r.getCode());
    }

    // ====== getReport 3 测例 ======
            @Test
    @DisplayName("getReport 返回 CPK + 测点")
    void testGetReport_OK() {
        CrmQualityCmm cmm = new CrmQualityCmm();
        cmm.setId(1L);
        cmm.setCpk(new BigDecimal("1.33"));
        cmm.setPdfUrl("/reports/cmm/QC20260612-0001.pdf");
        cmm.setDeviationAlert(0);
        when(cmmMapper.selectById(1L)).thenReturn(cmm);
        when(pointMapper.selectByCmmId(1L)).thenReturn(Collections.emptyList());

        Result<Map<String, Object>> r = service.getReport(1L);
        assertEquals(0, r.getCode());
        assertEquals(1.33, ((BigDecimal) r.getData().get("cpk")).doubleValue(), 0.01);
    }

    @Test
    @DisplayName("P1 修补 3：报告 PDF 必存")
    void testGetReport_PdfRequired() {
        CrmQualityCmm cmm = new CrmQualityCmm();
        cmm.setId(1L);
        cmm.setPdfUrl(null);
        when(cmmMapper.selectById(1L)).thenReturn(cmm);

        Result<Map<String, Object>> r = service.getReport(1L);
        assertEquals(40903, r.getCode());
        assertEquals("CMM_PDF_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("getReport 偏差超差告警")
    void testGetReport_DeviationAlert() {
        CrmQualityCmm cmm = new CrmQualityCmm();
        cmm.setId(1L);
        cmm.setCpk(new BigDecimal("0.45"));
        cmm.setPdfUrl("/test.pdf");
        cmm.setDeviationAlert(1);
        when(cmmMapper.selectById(1L)).thenReturn(cmm);
        when(pointMapper.selectByCmmId(1L)).thenReturn(Collections.emptyList());

        Result<Map<String, Object>> r = service.getReport(1L);
        assertEquals(0, r.getCode());
        assertEquals(true, r.getData().get("deviationAlert"));
    }

    // ====== listCmms 1 测例 ======
            @Test
    @DisplayName("listCmms 按 workOrderId 过滤")
    void testList() {
        CrmQualityCmm c = new CrmQualityCmm();
        c.setId(1L);
        when(cmmMapper.selectByWorkOrderId(1L)).thenReturn(List.of(c));
        Result<List<CrmQualityCmm>> r = service.listCmms(1L, null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    @Test
    @DisplayName("listCmms 按 result 过滤")
    void testList_ByResult() {
        CrmQualityCmm c = new CrmQualityCmm();
        c.setId(2L);
        when(cmmMapper.selectByResult("FAILED")).thenReturn(List.of(c));
        Result<List<CrmQualityCmm>> r = service.listCmms(null, "FAILED");
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }
}
