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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.31 · 品质·不良品处理 Service 单元测试（FR-7-4）
 * 14 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QualityDefectServiceTest {

    @Mock private CrmQualityDefectMapper defectMapper;
    @Mock private CrmQualityDefectHistoryMapper historyMapper;
    @Mock private CrmQualityDefectActionMapper actionMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private QualityDefectService service;

    @BeforeEach
    void setUp() {
        service = new QualityDefectService(defectMapper, historyMapper, actionMapper, docNoGenerator);
        when(docNoGenerator.nextQualityDefectNo())
                .thenReturn("QD20260612-0001", "QD20260612-0002", "QD20260612-0003", "QD20260612-0004");
        when(defectMapper.insert(any(CrmQualityDefect.class))).thenAnswer(inv -> {
            CrmQualityDefect d = inv.getArgument(0);
            d.setId(1L);
            return 1;
        });
        when(historyMapper.insert(any(CrmQualityDefectHistory.class))).thenAnswer(inv -> {
            CrmQualityDefectHistory h = inv.getArgument(0);
            h.setId(1L);
            return 1;
        });
        when(actionMapper.insert(any(CrmQualityDefectAction.class))).thenAnswer(inv -> {
            CrmQualityDefectAction a = inv.getArgument(0);
            a.setId(1L);
            return 1;
        });
        when(defectMapper.updateById(any(CrmQualityDefect.class))).thenReturn(1);
    }

    private DefectCreateRequest buildValidReq() {
        DefectCreateRequest req = new DefectCreateRequest();
        req.setSourceType("INTERNAL");
        req.setDefectType("尺寸超差");
        req.setSeverity("MAJOR");
        req.setQty(2);
        req.setTotalQty(100);
        req.setResponsibleDept("QA");
        return req;
    }

    // ====== createDefect 5 测例 ======
            @Test
    @DisplayName("createDefect happy path · 自动 PPM")
    void testCreate_OK() {
        Result<CrmQualityDefect> r = service.createDefect(buildValidReq(), 401L);
        assertEquals(0, r.getCode());
        assertEquals("QD20260612-0001", r.getData().getDefectNo());
        assertEquals(20000.0, r.getData().getDefectRatePpm().doubleValue(), 0.01);
    }

    @Test
    @DisplayName("createDefect 缺不良类型 → 40001")
    void testCreate_DefectTypeRequired() {
        DefectCreateRequest req = buildValidReq();
        req.setDefectType(null);
        Result<CrmQualityDefect> r = service.createDefect(req, 401L);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("P1 修补 2：缺责任部门 → 40001")
    void testCreate_ResponsibleDeptRequired() {
        DefectCreateRequest req = buildValidReq();
        req.setResponsibleDept(null);
        Result<CrmQualityDefect> r = service.createDefect(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("RESPONSIBLE_DEPT_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("createDefect 来源类型非法 → 40001")
    void testCreate_SourceTypeInvalid() {
        DefectCreateRequest req = buildValidReq();
        req.setSourceType("WRONG");
        Result<CrmQualityDefect> r = service.createDefect(req, 401L);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("createDefect 不良数量 ≤ 0 → 40001")
    void testCreate_QtyInvalid() {
        DefectCreateRequest req = buildValidReq();
        req.setQty(0);
        Result<CrmQualityDefect> r = service.createDefect(req, 401L);
        assertEquals(40001, r.getCode());
    }

    // ====== addAction 5 测例 ======
            @Test
    @DisplayName("P1 修补 1：动作 REWORK 接受")
    void testAddAction_Rework() {
        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(1L);
        d.setStatus("OPEN");
        when(defectMapper.selectById(1L)).thenReturn(d);

        AddDefectActionRequest req = new AddDefectActionRequest();
        req.setDefectId(1L);
        req.setActionType("REWORK");
        req.setQty(2);
        req.setResponsibleDept("QA");
        req.setCostAmount(new BigDecimal("5000"));
        Result<CrmQualityDefectAction> r = service.addAction(req, 401L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("P1 修补 1：动作 SCRAP 接受")
    void testAddAction_Scrap() {
        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(2L);
        d.setStatus("OPEN");
        when(defectMapper.selectById(2L)).thenReturn(d);

        AddDefectActionRequest req = new AddDefectActionRequest();
        req.setDefectId(2L);
        req.setActionType("SCRAP");
        req.setQty(1);
        req.setResponsibleDept("委外");
        req.setCostAmount(new BigDecimal("800"));
        Result<CrmQualityDefectAction> r = service.addAction(req, 401L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("P1 修补 1：动作 CONCESSION 接受")
    void testAddAction_Concession() {
        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(3L);
        d.setStatus("OPEN");
        when(defectMapper.selectById(3L)).thenReturn(d);

        AddDefectActionRequest req = new AddDefectActionRequest();
        req.setDefectId(3L);
        req.setActionType("CONCESSION");
        req.setQty(2);
        req.setResponsibleDept("QA");
        req.setCostAmount(new BigDecimal("600"));
        Result<CrmQualityDefectAction> r = service.addAction(req, 401L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("P1 修补 1：动作非法 → 40001")
    void testAddAction_ActionTypeInvalid() {
        AddDefectActionRequest req = new AddDefectActionRequest();
        req.setDefectId(1L);
        req.setActionType("WRONG");
        req.setQty(1);
        req.setResponsibleDept("QA");
        Result<CrmQualityDefectAction> r = service.addAction(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("ACTION_TYPE_INVALID", r.getMessage());
    }

    @Test
    @DisplayName("P1 修补 3：成本负数 → 40001")
    void testAddAction_CostNegative() {
        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(4L);
        d.setStatus("OPEN");
        when(defectMapper.selectById(4L)).thenReturn(d);

        AddDefectActionRequest req = new AddDefectActionRequest();
        req.setDefectId(4L);
        req.setActionType("REWORK");
        req.setQty(1);
        req.setResponsibleDept("QA");
        req.setCostAmount(new BigDecimal("-100"));
        Result<CrmQualityDefectAction> r = service.addAction(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("ACTION_COST_NEGATIVE", r.getMessage());
    }

    // ====== resolve 3 测例 ======
            @Test
    @DisplayName("resolve 已加动作 → RESOLVED + PPM 返回")
    void testResolve_OK() {
        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(1L);
        d.setStatus("IN_PROGRESS");
        d.setResult("REWORK");
        d.setDefectRatePpm(new BigDecimal("20000"));
        when(defectMapper.selectById(1L)).thenReturn(d);

        Result<Map<String, Object>> r = service.resolve(1L, "已解决", 401L);
        assertEquals(0, r.getCode());
        assertEquals("REWORK", r.getData().get("result"));
    }

    @Test
    @DisplayName("resolve 无动作 → 40903")
    void testResolve_NoAction() {
        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(2L);
        d.setStatus("OPEN");
        d.setResult(null);
        when(defectMapper.selectById(2L)).thenReturn(d);

        Result<Map<String, Object>> r = service.resolve(2L, null, 401L);
        assertEquals(40903, r.getCode());
        assertEquals("DEFECT_NO_ACTION", r.getMessage());
    }

    @Test
    @DisplayName("resolve 已关闭 → 40903")
    void testResolve_AlreadyClosed() {
        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(3L);
        d.setStatus("CLOSED");
        when(defectMapper.selectById(3L)).thenReturn(d);

        Result<Map<String, Object>> r = service.resolve(3L, null, 401L);
        assertEquals(40903, r.getCode());
    }

    // ====== list 1 测例 ======
            @Test
    @DisplayName("list 按 status 过滤")
    void testList() {
        CrmQualityDefect d = new CrmQualityDefect();
        d.setId(1L);
        when(defectMapper.selectByStatus("OPEN")).thenReturn(List.of(d));
        Result<List<CrmQualityDefect>> r = service.list(null, "OPEN", null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }
}
