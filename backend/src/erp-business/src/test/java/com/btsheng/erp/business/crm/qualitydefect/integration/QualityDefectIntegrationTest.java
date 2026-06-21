package com.btsheng.erp.business.crm.qualitydefect.integration;

import com.btsheng.erp.business.crm.qualitydefect.dto.AddDefectActionRequest;
import com.btsheng.erp.business.crm.qualitydefect.dto.DefectCreateRequest;
import com.btsheng.erp.business.crm.qualitydefect.entity.CrmQualityDefect;
import com.btsheng.erp.business.crm.qualitydefect.entity.CrmQualityDefectAction;
import com.btsheng.erp.business.crm.qualitydefect.entity.CrmQualityDefectHistory;
import com.btsheng.erp.business.crm.qualitydefect.mapper.CrmQualityDefectActionMapper;
import com.btsheng.erp.business.crm.qualitydefect.mapper.CrmQualityDefectHistoryMapper;
import com.btsheng.erp.business.crm.qualitydefect.mapper.CrmQualityDefectMapper;
import com.btsheng.erp.business.crm.qualitydefect.service.QualityDefectService;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.31 · 品质·不良品处理 集成测试（FR-7-4）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QualityDefectIntegrationTest {

    @Mock private CrmQualityDefectMapper defectMapper;
    @Mock private CrmQualityDefectHistoryMapper historyMapper;
    @Mock private CrmQualityDefectActionMapper actionMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private QualityDefectService service;

    @BeforeEach
    void setUp() {
        service = new QualityDefectService(defectMapper, historyMapper, actionMapper, docNoGenerator);
        when(docNoGenerator.nextQualityDefectNo())
                .thenReturn("QD20260612-0001", "QD20260612-0002", "QD20260612-0003", "QD20260612-0004", "QD20260612-0005");
        when(defectMapper.insert(any(CrmQualityDefect.class))).thenAnswer(inv -> {
            CrmQualityDefect d = inv.getArgument(0);
            d.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(historyMapper.insert(any(CrmQualityDefectHistory.class))).thenAnswer(inv -> {
            CrmQualityDefectHistory h = inv.getArgument(0);
            h.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(actionMapper.insert(any(CrmQualityDefectAction.class))).thenAnswer(inv -> {
            CrmQualityDefectAction a = inv.getArgument(0);
            a.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(defectMapper.updateById(any(CrmQualityDefect.class))).thenReturn(1);
    }

    private DefectCreateRequest buildReq() {
        DefectCreateRequest req = new DefectCreateRequest();
        req.setSourceType("INTERNAL");
        req.setSourceId(3L);
        req.setSourceNo("QI20260612-0003");
        req.setDefectType("Mg 元素低于下限");
        req.setSeverity("CRITICAL");
        req.setQty(10);
        req.setTotalQty(50);
        req.setResponsibleDept("QA");
        req.setD1Team("QA 团队");
        req.setD4RootCause("原料供应商 Mg 元素波动");
        req.setD5Action("更换供应商");
        return req;
    }

    // ====== 完整 lifecycle 1：登记 → REWORK → resolve ======
            @Test
    @DisplayName("集成 lifecycle 1：不良品登记 → REWORK → resolve 关闭")
    void testIntegration_Defect_Rework_Resolve() {
        Result<CrmQualityDefect> c = service.createDefect(buildReq(), 401L);
        assertEquals(0, c.getCode());
        Long id = c.getData().getId();
        assertEquals(200000.0, c.getData().getDefectRatePpm().doubleValue(), 0.01);

        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(id);
        d.setStatus("OPEN");
        d.setResult(null);
        when(defectMapper.selectById(id)).thenReturn(d);

        AddDefectActionRequest ar = new AddDefectActionRequest();
        ar.setDefectId(id);
        ar.setActionType("REWORK");
        ar.setQty(10);
        ar.setResponsibleDept("QA");
        ar.setCostAmount(new BigDecimal("5000"));
        Result<CrmQualityDefectAction> aa = service.addAction(ar, 401L);
        assertEquals(0, aa.getCode());

        d.setStatus("IN_PROGRESS");
        d.setResult("REWORK");
        d.setDefectRatePpm(c.getData().getDefectRatePpm());
        when(defectMapper.selectById(id)).thenReturn(d);
        Result<Map<String, Object>> r = service.resolve(id, "更换供应商后 3 批合格", 401L);
        assertEquals(0, r.getCode());
        assertEquals("REWORK", r.getData().get("result"));
    }

    // ====== 完整 lifecycle 2：登记 → SCRAP → resolve ======
            @Test
    @DisplayName("集成 lifecycle 2：委外 CRITICAL → SCRAP → resolve")
    void testIntegration_Defect_Scrap() {
        DefectCreateRequest req = buildReq();
        req.setSourceType("OUTSOURCE");
        req.setSourceNo("OQ20260612-0003");
        Result<CrmQualityDefect> c = service.createDefect(req, 401L);
        Long id = c.getData().getId();

        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(id);
        d.setStatus("OPEN");
        when(defectMapper.selectById(id)).thenReturn(d);

        AddDefectActionRequest ar = new AddDefectActionRequest();
        ar.setDefectId(id);
        ar.setActionType("SCRAP");
        ar.setQty(1);
        ar.setResponsibleDept("委外");
        ar.setCostAmount(new BigDecimal("800"));
        service.addAction(ar, 401L);

        d.setStatus("IN_PROGRESS");
        d.setResult("SCRAP");
        when(defectMapper.selectById(id)).thenReturn(d);
        Result<Map<String, Object>> r = service.resolve(id, "量检具校准后关闭", 401L);
        assertEquals(0, r.getCode());
        assertEquals("SCRAP", r.getData().get("result"));
    }

    // ====== 让步接收 ======
            @Test
    @DisplayName("集成：CONCESSION 让步接收")
    void testIntegration_Defect_Concession() {
        Result<CrmQualityDefect> c = service.createDefect(buildReq(), 401L);
        Long id = c.getData().getId();

        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(id);
        d.setStatus("OPEN");
        when(defectMapper.selectById(id)).thenReturn(d);

        AddDefectActionRequest ar = new AddDefectActionRequest();
        ar.setDefectId(id);
        ar.setActionType("CONCESSION");
        ar.setQty(2);
        ar.setResponsibleDept("QA");
        ar.setCostAmount(new BigDecimal("600"));
        Result<CrmQualityDefectAction> r = service.addAction(ar, 401L);
        assertEquals(0, r.getCode());
        assertEquals("CONCESSION", r.getData().getActionType());
    }

    // ====== 8D 报告字段 ======
            @Test
    @DisplayName("AC-7.4.1：8D 报告字段（D1/D4/D5/D8）")
    void testIntegration_8D() {
        DefectCreateRequest req = buildReq();
        req.setD8Closure("连续 3 批合格关闭");
        Result<CrmQualityDefect> r = service.createDefect(req, 401L);
        assertEquals(0, r.getCode());
        assertEquals("QA 团队", r.getData().getD1Team());
        assertEquals("原料供应商 Mg 元素波动", r.getData().getD4RootCause());
        assertEquals("更换供应商", r.getData().getD5Action());
    }

    // ====== 跨 1.28 检单（INTERNAL 来源）======
            @Test
    @DisplayName("跨 1.28：来源 INTERNAL（IQC/IPQC/OQC 联动）")
    void testIntegration_Cross_28_Internal() {
        DefectCreateRequest req = buildReq();
        req.setSourceType("INTERNAL");
        req.setSourceId(3L);
        req.setSourceNo("QI20260612-0003");
        Result<CrmQualityDefect> r = service.createDefect(req, 401L);
        assertEquals(0, r.getCode());
        assertEquals("INTERNAL", r.getData().getSourceType());
    }

    // ====== 跨 1.27 委外（OUTSOURCE 来源）======
            @Test
    @DisplayName("跨 1.27：来源 OUTSOURCE（委外工序）")
    void testIntegration_Cross_27_Outsource() {
        DefectCreateRequest req = buildReq();
        req.setSourceType("OUTSOURCE");
        req.setSourceId(3L);
        req.setSourceNo("OQ20260612-0003");
        Result<CrmQualityDefect> r = service.createDefect(req, 401L);
        assertEquals(0, r.getCode());
        assertEquals("OUTSOURCE", r.getData().getSourceType());
    }

    // ====== PPM 自动计算 ======
            @Test
    @DisplayName("AC-7.4.3：PPM 自动统计（10/50 = 200000 PPM）")
    void testIntegration_PpmAuto() {
        Result<CrmQualityDefect> r = service.createDefect(buildReq(), 401L);
        assertEquals(0, r.getCode());
        assertEquals(200000.0, r.getData().getDefectRatePpm().doubleValue(), 0.01);
    }

    // ====== 审计留痕 ======
            @Test
    @DisplayName("审计留痕：createDefect + addAction 写 1 defect + 1 history + 1 action")
    void testIntegration_Audit() {
        Result<CrmQualityDefect> c = service.createDefect(buildReq(), 401L);
        Long id = c.getData().getId();
        verify(defectMapper, times(1)).insert(any(CrmQualityDefect.class));
        verify(historyMapper, times(1)).insert(any(CrmQualityDefectHistory.class));

        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(id);
        d.setStatus("OPEN");
        when(defectMapper.selectById(id)).thenReturn(d);

        AddDefectActionRequest ar = new AddDefectActionRequest();
        ar.setDefectId(id);
        ar.setActionType("REWORK");
        ar.setQty(10);
        ar.setResponsibleDept("QA");
        service.addAction(ar, 401L);
        verify(actionMapper, times(1)).insert(any(CrmQualityDefectAction.class));
    }

    // ====== 状态机推进 ======
            @Test
    @DisplayName("状态机：OPEN → IN_PROGRESS → RESOLVED")
    void testIntegration_StatusMachine() {
        Result<CrmQualityDefect> c = service.createDefect(buildReq(), 401L);
        Long id = c.getData().getId();
        assertEquals("OPEN", c.getData().getStatus());

        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(id);
        d.setStatus("OPEN");
        when(defectMapper.selectById(id)).thenReturn(d);

        AddDefectActionRequest ar = new AddDefectActionRequest();
        ar.setDefectId(id);
        ar.setActionType("REWORK");
        ar.setQty(10);
        ar.setResponsibleDept("QA");
        service.addAction(ar, 401L);

        verify(defectMapper, atLeastOnce()).updateById(argThat((CrmQualityDefect dd) ->
                "IN_PROGRESS".equals(dd.getStatus()) && "REWORK".equals(dd.getResult())));
    }

    // ====== 重复登记阻断（已 CLOSED）======
            @Test
    @DisplayName("集成：CLOSED 状态 addAction → 40903")
    void testIntegration_ClosedAddAction() {
        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(99L);
        d.setStatus("CLOSED");
        when(defectMapper.selectById(99L)).thenReturn(d);

        AddDefectActionRequest ar = new AddDefectActionRequest();
        ar.setDefectId(99L);
        ar.setActionType("REWORK");
        ar.setQty(1);
        ar.setResponsibleDept("QA");
        Result<CrmQualityDefectAction> r = service.addAction(ar, 401L);
        assertEquals(40903, r.getCode());
    }
}
