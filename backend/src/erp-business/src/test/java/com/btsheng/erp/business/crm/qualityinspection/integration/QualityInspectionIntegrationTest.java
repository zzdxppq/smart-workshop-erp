package com.btsheng.erp.business.crm.qualityinspection.integration;

import com.btsheng.erp.business.crm.qualityinspection.dto.AddInspectionItemRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionCreateRequest;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspection;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspectionItem;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualitySample;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityInspectionItemMapper;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityInspectionMapper;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualitySampleMapper;
import com.btsheng.erp.business.crm.qualityinspection.service.QualityInspectionService;
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
 * V1.3.7 · Story 1.28 · 品质·来料/过程/成品检 集成测试（FR-7-1）
 * 12 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QualityInspectionIntegrationTest {

    @Mock private CrmQualityInspectionMapper inspectionMapper;
    @Mock private CrmQualityInspectionItemMapper itemMapper;
    @Mock private CrmQualitySampleMapper sampleMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private QualityInspectionService service;

    @BeforeEach
    void setUp() {
        service = new QualityInspectionService(inspectionMapper, itemMapper, sampleMapper, docNoGenerator);
        when(docNoGenerator.nextQualityInspectionNo())
                .thenReturn("QI20260612-0001", "QI20260612-0002", "QI20260612-0003",
                        "QI20260612-0004", "QI20260612-0005");
        when(inspectionMapper.insert(any(CrmQualityInspection.class))).thenAnswer(inv -> {
            CrmQualityInspection q = inv.getArgument(0);
            q.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(itemMapper.insert(any(CrmQualityInspectionItem.class))).thenAnswer(inv -> {
            CrmQualityInspectionItem i = inv.getArgument(0);
            i.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(sampleMapper.insert(any(CrmQualitySample.class))).thenAnswer(inv -> {
            CrmQualitySample s = inv.getArgument(0);
            s.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(inspectionMapper.updateById(any(CrmQualityInspection.class))).thenReturn(1);
    }

    private InspectionCreateRequest.InspectionItemDto item(String name, String sev, int passed) {
        InspectionCreateRequest.InspectionItemDto i = new InspectionCreateRequest.InspectionItemDto();
        i.setItemName(name);
        i.setSeverity(sev);
        i.setPassed(passed);
        return i;
    }

    // ====== 完整 lifecycle 1：IQC 来料 → PASSED ======
            @Test
    @DisplayName("集成 lifecycle 1：IQC 来料检 → PASSED")
    void testIntegration_IQC_Pass() {
        InspectionCreateRequest req = new InspectionCreateRequest();
        req.setInspectType("IQC");
        req.setMaterialId(1001L);
        req.setMaterialCode("M-Q235");
        req.setLotSize(500);
        req.setSampleSize(50);
        req.setAqlLevel("1.0");
        req.setItems(Arrays.asList(item("外观", "INFO", 1), item("厚度", "INFO", 1)));

        Result<CrmQualityInspection> c = service.createInspection(req, 401L);
        assertEquals(0, c.getCode());
        Long id = c.getData().getId();

        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(id);
        q.setInspectType("IQC");
        q.setInspectQty(2);
        q.setFailedQty(0);
        q.setMaxSeverity("INFO");
        q.setResult("DRAFT");
        when(inspectionMapper.selectById(id)).thenReturn(q);

        Result<Map<String, Object>> p = service.pass(id, 401L);
        assertEquals(0, p.getCode());
        assertEquals("PASSED", ((CrmQualityInspection) p.getData().get("inspection")).getResult());
    }

    // ====== 完整 lifecycle 2：OQC 成品 → 通过 → 触发入库 ======
            @Test
    @DisplayName("集成 lifecycle 2：OQC 成品检 → PASSED → 触发入库")
    void testIntegration_OQC_Stockin() {
        InspectionCreateRequest req = new InspectionCreateRequest();
        req.setInspectType("OQC");
        req.setWorkOrderId(1L);
        req.setWorkOrderNo("GD20260608-0001");
        req.setItems(Arrays.asList(item("终检外观", "INFO", 1), item("尺寸", "INFO", 1)));

        Result<CrmQualityInspection> c = service.createInspection(req, 401L);
        Long id = c.getData().getId();

        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(id);
        q.setInspectType("OQC");
        q.setWorkOrderId(1L);
        q.setInspectQty(2);
        q.setMaxSeverity("INFO");
        q.setResult("DRAFT");
        when(inspectionMapper.selectById(id)).thenReturn(q);

        Result<Map<String, Object>> p = service.pass(id, 401L);
        assertEquals(0, p.getCode());
        assertEquals(true, p.getData().get("triggerStockin"));
        verify(inspectionMapper, atLeastOnce()).updateById(argThat((CrmQualityInspection qq) ->
                qq.getTriggerStockin() != null && qq.getTriggerStockin() == 1));
    }

    // ====== 完整 lifecycle 3：IQC → CRITICAL → reject 触发返修 ======
            @Test
    @DisplayName("集成 lifecycle 3：IQC → CRITICAL → reject → 触发返修")
    void testIntegration_IQC_Critical_Reject() {
        InspectionCreateRequest req = new InspectionCreateRequest();
        req.setInspectType("IQC");
        req.setMaterialId(1003L);
        req.setItems(Arrays.asList(item("化学成分", "CRITICAL", 0)));

        Result<CrmQualityInspection> c = service.createInspection(req, 401L);
        Long id = c.getData().getId();

        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(id);
        q.setInspectType("IQC");
        q.setMaterialId(1003L);
        q.setInspectQty(1);
        q.setMaxSeverity("CRITICAL");
        q.setResult("DRAFT");
        when(inspectionMapper.selectById(id)).thenReturn(q);

        // CRITICAL 阻止 PASS
            Result<Map<String, Object>> p = service.pass(id, 401L);
        assertEquals(40903, p.getCode());

        // reject 触发返修
            Result<Map<String, Object>> r = service.reject(id, "化学成分不合格", 401L);
        assertEquals(0, r.getCode());
        assertEquals(true, r.getData().get("triggerRework"));
    }

    // ====== 抽样规则 AQL ======
            @Test
    @DisplayName("P1 修补 1：AQL-1.0 接受 / AQL-WRONG 拒绝")
    void testIntegration_AQL() {
        InspectionCreateRequest ok = new InspectionCreateRequest();
        ok.setInspectType("IQC");
        ok.setMaterialId(1001L);
        ok.setAqlLevel("1.0");
        ok.setItems(Arrays.asList(item("外观", "INFO", 1)));
        assertEquals(0, service.createInspection(ok, 401L).getCode());

        InspectionCreateRequest bad = new InspectionCreateRequest();
        bad.setInspectType("IQC");
        bad.setMaterialId(1001L);
        bad.setAqlLevel("WRONG");
        bad.setItems(Arrays.asList(item("外观", "INFO", 1)));
        assertEquals(40001, service.createInspection(bad, 401L).getCode());
    }

    // ====== 严重度 4 级 ======
            @Test
    @DisplayName("P1 修补 3：严重度 4 级（INFO/WARN/ERROR/CRITICAL）全部接受")
    void testIntegration_4Severity() {
        for (String sev : new String[]{"INFO", "WARN", "ERROR", "CRITICAL"}) {
            InspectionCreateRequest req = new InspectionCreateRequest();
            req.setInspectType("IQC");
            req.setMaterialId(1001L);
            req.setItems(Arrays.asList(item("test-" + sev, sev, 0)));
            Result<CrmQualityInspection> r = service.createInspection(req, 401L);
            assertEquals(0, r.getCode(), "严重度 " + sev + " 必接受");
        }
    }

    // ====== IPQC 过程检 ======
            @Test
    @DisplayName("IPQC 过程检 → 缺工序 → 40001")
    void testIntegration_IPQC_ProcessRequired() {
        InspectionCreateRequest req = new InspectionCreateRequest();
        req.setInspectType("IPQC");
        req.setWorkOrderId(1L);
        req.setItems(Arrays.asList(item("尺寸", "INFO", 1)));
        Result<CrmQualityInspection> r = service.createInspection(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("IPQC_PROCESS_REQUIRED", r.getMessage());
    }

    // ====== 跨检 1.28 ↔ 1.25 委外（区别） ======
            @Test
    @DisplayName("区别于 1.25 委外：3 检为 IQC/IPQC/OQC，不是 OI")
    void testIntegration_3Types() {
        for (String type : new String[]{"IQC", "IPQC", "OQC"}) {
            InspectionCreateRequest req = new InspectionCreateRequest();
            req.setInspectType(type);
            if ("IQC".equals(type)) {
                req.setMaterialId(1001L);
            } else {
                req.setWorkOrderId(1L);
                if ("IPQC".equals(type)) {
                    req.setProcessName("粗车");
                }
            }
            req.setItems(Arrays.asList(item("外观", "INFO", 1)));
            Result<CrmQualityInspection> r = service.createInspection(req, 401L);
            assertEquals(0, r.getCode());
            assertEquals(type, r.getData().getInspectType());
        }
    }

    // ====== 跨 1.13 批次 ======
            @Test
    @DisplayName("跨 1.13：携带 batchNo")
    void testIntegration_Cross_13_Batch() {
        InspectionCreateRequest req = new InspectionCreateRequest();
        req.setInspectType("IQC");
        req.setMaterialId(1001L);
        req.setBatchNo("B20260610-0001");
        req.setItems(Arrays.asList(item("外观", "INFO", 1)));
        Result<CrmQualityInspection> r = service.createInspection(req, 401L);
        assertEquals(0, r.getCode());
        assertEquals("B20260610-0001", r.getData().getBatchNo());
    }

    // ====== 审计留痕 ======
            @Test
    @DisplayName("审计留痕：createInspection 写 1 inspection + N items")
    void testIntegration_Audit() {
        InspectionCreateRequest req = new InspectionCreateRequest();
        req.setInspectType("IQC");
        req.setMaterialId(1001L);
        req.setItems(Arrays.asList(
                item("外观", "INFO", 1),
                item("厚度", "INFO", 1),
                item("材质", "WARN", 0)));

        service.createInspection(req, 401L);
        verify(inspectionMapper, times(1)).insert(any(CrmQualityInspection.class));
        verify(itemMapper, times(3)).insert(any(CrmQualityInspectionItem.class));
    }

    // ====== addItem 同步更新 max_severity ======
            @Test
    @DisplayName("addItem 追加 ERROR → max_severity 升级")
    void testIntegration_AddItem_Upgrade() {
        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(500L);
        q.setInspectQty(1);
        q.setFailedQty(0);
        q.setMaxSeverity("INFO");
        when(inspectionMapper.selectById(500L)).thenReturn(q);

        AddInspectionItemRequest req = new AddInspectionItemRequest();
        req.setInspectionId(500L);
        req.setItemName("色差");
        req.setSeverity("ERROR");
        req.setPassed(0);
        service.addItem(req, 401L);

        verify(inspectionMapper, atLeastOnce()).updateById(argThat((CrmQualityInspection qq) ->
                "ERROR".equals(qq.getMaxSeverity())));
    }

    // ====== 重复 PASS 阻断 ======
            @Test
    @DisplayName("集成：重复 PASS → 40903")
    void testIntegration_RePass_Blocked() {
        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(600L);
        q.setResult("PASSED");
        when(inspectionMapper.selectById(600L)).thenReturn(q);

        Result<Map<String, Object>> r = service.pass(600L, 401L);
        assertEquals(40903, r.getCode());
    }

    // ====== 抽样记录 ======
            @Test
    @DisplayName("P1 修补 1：抽样记录写入")
    void testIntegration_SampleRecord() {
        InspectionCreateRequest req = new InspectionCreateRequest();
        req.setInspectType("IQC");
        req.setMaterialId(1001L);
        req.setItems(Arrays.asList(item("外观", "INFO", 1)));
        InspectionCreateRequest.SampleDto s = new InspectionCreateRequest.SampleDto();
        s.setSampleNo("S001");
        s.setSampleQty(50);
        s.setDefectQty(0);
        s.setAqlPassed(1);
        s.setRemark("AQL-1.0 通过");
        req.setSamples(java.util.Collections.singletonList(s));

        service.createInspection(req, 401L);
        verify(sampleMapper, times(1)).insert(any(CrmQualitySample.class));
    }
}
