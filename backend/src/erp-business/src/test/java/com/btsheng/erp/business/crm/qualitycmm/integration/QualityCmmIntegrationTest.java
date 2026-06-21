package com.btsheng.erp.business.crm.qualitycmm.integration;

import com.btsheng.erp.business.crm.qualitycmm.dto.AddCmmPointRequest;
import com.btsheng.erp.business.crm.qualitycmm.dto.CmmCreateRequest;
import com.btsheng.erp.business.crm.qualitycmm.entity.CrmQualityCmm;
import com.btsheng.erp.business.crm.qualitycmm.entity.CrmQualityCmmPoint;
import com.btsheng.erp.business.crm.qualitycmm.mapper.CrmQualityCmmMapper;
import com.btsheng.erp.business.crm.qualitycmm.mapper.CrmQualityCmmPointMapper;
import com.btsheng.erp.business.crm.qualitycmm.service.QualityCmmService;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.30 · 品质·CMM 三次元 集成测试（FR-7-3）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QualityCmmIntegrationTest {

    @Mock private CrmQualityCmmMapper cmmMapper;
    @Mock private CrmQualityCmmPointMapper pointMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private QualityCmmService service;

    @BeforeEach
    void setUp() {
        service = new QualityCmmService(cmmMapper, pointMapper, docNoGenerator);
        when(docNoGenerator.nextQualityCmmNo())
                .thenReturn("QC20260612-0001", "QC20260612-0002", "QC20260612-0003",
                        "QC20260612-0004", "QC20260612-0005");
        when(cmmMapper.insert(any(CrmQualityCmm.class))).thenAnswer(inv -> {
            CrmQualityCmm c = inv.getArgument(0);
            c.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(pointMapper.insert(any(CrmQualityCmmPoint.class))).thenAnswer(inv -> {
            CrmQualityCmmPoint p = inv.getArgument(0);
            p.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(cmmMapper.updateById(any(CrmQualityCmm.class))).thenReturn(1);
    }

    private CmmCreateRequest.CmmPointDto point(String no, String nominal, String measured) {
        CmmCreateRequest.CmmPointDto p = new CmmCreateRequest.CmmPointDto();
        p.setPointNo(no);
        p.setAxis("X");
        p.setNominalValue(new BigDecimal(nominal));
        p.setMeasuredValue(new BigDecimal(measured));
        p.setToleranceUpper(new BigDecimal("0.0500"));
        p.setToleranceLower(new BigDecimal("-0.0500"));
        return p;
    }

    private CmmCreateRequest buildReq() {
        CmmCreateRequest req = new CmmCreateRequest();
        req.setWorkOrderId(1L);
        req.setDrawingNo("DWG-001");
        req.setPartName("法兰盘");
        req.setPdfUrl("/reports/cmm/test.pdf");
        req.setPoints(Arrays.asList(
                point("P1", "50.0000", "50.0050"),
                point("P2", "30.0000", "30.0020"),
                point("P3", "10.0000", "10.0010")));
        return req;
    }

    // ====== 完整 lifecycle 1：CMM → 报告 ======
            @Test
    @DisplayName("集成 lifecycle 1：CMM 创建 → 报告")
    void testIntegration_CMM_Report() {
        Result<CrmQualityCmm> c = service.createCmm(buildReq(), 401L);
        assertEquals(0, c.getCode());
        Long id = c.getData().getId();

        CrmQualityCmm cmm = new CrmQualityCmm();
        cmm.setId(id);
        cmm.setCmmNo("QC20260612-0001");
        cmm.setCpk(new BigDecimal("1.33"));
        cmm.setPdfUrl("/reports/cmm/test.pdf");
        cmm.setDeviationAlert(0);
        when(cmmMapper.selectById(id)).thenReturn(cmm);
        when(pointMapper.selectByCmmId(id)).thenReturn(java.util.Collections.emptyList());

        Result<Map<String, Object>> r = service.getReport(id);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().get("cpk"));
    }

    // ====== 偏差超差告警 ======
            @Test
    @DisplayName("P1 修补 2：偏差 > 0.1 → deviation_alert = 1")
    void testIntegration_DeviationAlert() {
        CmmCreateRequest req = buildReq();
        req.setPoints(Arrays.asList(
                point("P1", "50.0000", "50.1500"),
                point("P2", "30.0000", "30.0200"),
                point("P3", "10.0000", "10.0010")));
        Result<CrmQualityCmm> r = service.createCmm(req, 401L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().getDeviationAlert());
    }

    // ====== 测点 < 3 拒绝 ======
            @Test
    @DisplayName("P1 修补 1：测点 = 2 → 40001")
    void testIntegration_Points2_Reject() {
        CmmCreateRequest req = buildReq();
        req.setPoints(Arrays.asList(
                point("P1", "50.0000", "50.0050"),
                point("P2", "30.0000", "30.0020")));
        Result<CrmQualityCmm> r = service.createCmm(req, 401L);
        assertEquals(40001, r.getCode());
    }

    // ====== 测点 = 3 接受 ======
            @Test
    @DisplayName("测点 = 3 接受")
    void testIntegration_Points3_Accept() {
        Result<CrmQualityCmm> r = service.createCmm(buildReq(), 401L);
        assertEquals(0, r.getCode());
        assertEquals(3, r.getData().getPointCount());
    }

    // ====== CPK 4 指标 ======
            @Test
    @DisplayName("AC-7.3.2：CPK 4 指标（Pp/Ppk/Cp/Cpk）")
    void testIntegration_CPK() {
        Result<CrmQualityCmm> r = service.createCmm(buildReq(), 401L);
        assertEquals(0, r.getCode());
        // 小偏差 → Cpk 1.33
            assertEquals(1.33, r.getData().getCpk().doubleValue(), 0.01);
        assertNotNull(r.getData().getPp());
        assertNotNull(r.getData().getPpk());
        assertNotNull(r.getData().getCp());
    }

    // ====== 报告 PDF 必存 ======
            @Test
    @DisplayName("P1 修补 3：报告 PDF 必存")
    void testIntegration_PdfRequired() {
        CmmCreateRequest req = buildReq();
        req.setPdfUrl(null);
        Result<CrmQualityCmm> c = service.createCmm(req, 401L);
        Long id = c.getData().getId();

        CrmQualityCmm cmm = new CrmQualityCmm();
        cmm.setId(id);
        cmm.setPdfUrl(null);
        when(cmmMapper.selectById(id)).thenReturn(cmm);
        Result<Map<String, Object>> r = service.getReport(id);
        assertEquals(40903, r.getCode());
    }

    // ====== 跨 1.7 图号 ======
            @Test
    @DisplayName("跨 1.7：CMM 携带 drawingNo")
    void testIntegration_Cross_07_Drawing() {
        CmmCreateRequest req = buildReq();
        req.setDrawingNo("DWG-2024-001");
        Result<CrmQualityCmm> r = service.createCmm(req, 401L);
        assertEquals(0, r.getCode());
        assertEquals("DWG-2024-001", r.getData().getDrawingNo());
    }

    // ====== addPoint 同步更新 ======
            @Test
    @DisplayName("addPoint → 同步 point_count + max_deviation")
    void testIntegration_AddPoint_Sync() {
        CrmQualityCmm cmm = new CrmQualityCmm();
        cmm.setId(800L);
        cmm.setPointCount(3);
        cmm.setMaxDeviation(new BigDecimal("0.0050"));
        cmm.setDeviationAlert(0);
        when(cmmMapper.selectById(800L)).thenReturn(cmm);

        AddCmmPointRequest req = new AddCmmPointRequest();
        req.setCmmId(800L);
        req.setPointNo("P4");
        req.setAxis("Y");
        req.setNominalValue(new BigDecimal("30.0000"));
        req.setMeasuredValue(new BigDecimal("30.0800"));
        req.setToleranceUpper(new BigDecimal("0.0500"));
        req.setToleranceLower(new BigDecimal("-0.0500"));
        service.addPoint(req, 401L);

        verify(cmmMapper, atLeastOnce()).updateById(argThat((CrmQualityCmm cc) ->
                cc.getPointCount() != null && cc.getPointCount() == 4
                && cc.getDeviationAlert() != null && cc.getDeviationAlert() == 1));
    }

    // ====== 区别于 1.27 委外 CMM ======
            @Test
    @DisplayName("区别于 1.27 委外 CMM：QC 模板且含 CPK 4 指标")
    void testIntegration_VS_27() {
        Result<CrmQualityCmm> r = service.createCmm(buildReq(), 401L);
        assertTrue(r.getData().getCmmNo().startsWith("QC"));
        assertNotNull(r.getData().getCpk());
    }

    // ====== 审计留痕 ======
            @Test
    @DisplayName("审计留痕：createCmm 写 1 cmm + N points")
    void testIntegration_Audit() {
        service.createCmm(buildReq(), 401L);
        verify(cmmMapper, times(1)).insert(any(CrmQualityCmm.class));
        verify(pointMapper, times(3)).insert(any(CrmQualityCmmPoint.class));
    }
}
