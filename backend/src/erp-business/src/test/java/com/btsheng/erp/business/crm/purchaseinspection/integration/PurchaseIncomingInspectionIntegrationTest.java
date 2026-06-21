package com.btsheng.erp.business.crm.purchaseinspection.integration;

import com.btsheng.erp.business.crm.purchaseinspection.dto.AddItemRequest;
import com.btsheng.erp.business.crm.purchaseinspection.dto.CreateInspectionRequest;
import com.btsheng.erp.business.crm.purchaseinspection.dto.SubmitResultRequest;
import com.btsheng.erp.business.crm.purchaseinspection.entity.CrmPurchaseIncomingInspection;
import com.btsheng.erp.business.crm.purchaseinspection.entity.CrmPurchaseIncomingItem;
import com.btsheng.erp.business.crm.purchaseinspection.mapper.CrmPurchaseIncomingInspectionMapper;
import com.btsheng.erp.business.crm.purchaseinspection.mapper.CrmPurchaseIncomingItemMapper;
import com.btsheng.erp.business.crm.purchaseinspection.service.PurchaseIncomingInspectionService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * V1.3.7 · Story 1.35 · 采购·来料质检 集成测试（FR-8-4）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseIncomingInspectionIntegrationTest {

    @Mock private CrmPurchaseIncomingInspectionMapper inspectionMapper;
    @Mock private CrmPurchaseIncomingItemMapper itemMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private PurchaseIncomingInspectionService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseIncomingInspectionService(inspectionMapper, itemMapper, docNoGenerator);
        when(docNoGenerator.nextPurchaseIncomingInspectionNo())
                .thenReturn("PI20260612-0001", "PI20260612-0002", "PI20260612-0003", "PI20260612-0004", "PI20260612-0005");
        when(inspectionMapper.insert(any(CrmPurchaseIncomingInspection.class))).thenAnswer(inv -> {
            CrmPurchaseIncomingInspection i = inv.getArgument(0);
            i.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(itemMapper.insert(any(CrmPurchaseIncomingItem.class))).thenAnswer(inv -> {
            CrmPurchaseIncomingItem it = inv.getArgument(0);
            it.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(inspectionMapper.updateById(any(CrmPurchaseIncomingInspection.class))).thenReturn(1);
    }

    // ====== lifecycle 1：创建 → 3 项 → PASS ======
            @Test
    @DisplayName("lifecycle 1：创建 → 3 检验项 → PASS · 不良率 4%")
    void testIntegration_PassLifecycle() {
        when(inspectionMapper.selectByPoAndMaterial(any(), any())).thenReturn(null);
        CreateInspectionRequest cr = new CreateInspectionRequest();
        cr.setPoId(1L);
        cr.setPoNo("PO20260401-0001");
        cr.setMaterialId(1001L);
        cr.setMaterialCode("M-AL6061-PT");
        cr.setInspectorId(601L);
        cr.setSampleSize(50);
        cr.setNotifyEmail("inspect@btsheng-163.com");
        Result<CrmPurchaseIncomingInspection> c = service.create(cr, 601L);
        Long iid = c.getData().getId();

        when(inspectionMapper.selectById(iid)).thenReturn(c.getData());
        when(itemMapper.selectByInspectionId(iid)).thenReturn(new ArrayList<>());

        for (String item : new String[]{"外观", "尺寸", "材质"}) {
            AddItemRequest ar = new AddItemRequest();
            ar.setCheckItem(item);
            ar.setSampleQty(10);
            ar.setPassQty(9);
            ar.setFailQty(1);
            ar.setIsCritical(0);
            service.addItem(iid, ar);
        }
        SubmitResultRequest sr = new SubmitResultRequest();
        sr.setResult("PASS");
        Result<CrmPurchaseIncomingInspection> r = service.pass(iid, sr);
        assertEquals(0, r.getCode());
        assertEquals("PASS", r.getData().getResult());
    }

    // ====== lifecycle 2：关键项 FAIL → REJECT ======
            @Test
    @DisplayName("lifecycle 2：关键尺寸 FAIL → 整单 REJECT")
    void testIntegration_RejectByCritical() {
        when(inspectionMapper.selectByPoAndMaterial(any(), any())).thenReturn(null);
        CreateInspectionRequest cr = new CreateInspectionRequest();
        cr.setPoId(2L);
        cr.setPoNo("PO20260410-0005");
        cr.setMaterialId(1002L);
        cr.setInspectorId(601L);
        cr.setSampleSize(50);
        cr.setNotifyEmail("inspect@btsheng-163.com");
        Result<CrmPurchaseIncomingInspection> c = service.create(cr, 601L);
        Long iid = c.getData().getId();
        when(inspectionMapper.selectById(iid)).thenReturn(c.getData());
        when(itemMapper.selectByInspectionId(iid)).thenReturn(new ArrayList<>());

        AddItemRequest ar = new AddItemRequest();
        ar.setCheckItem("尺寸");
        ar.setSampleQty(20);
        ar.setPassQty(15);
        ar.setFailQty(5);
        ar.setIsCritical(1);
        service.addItem(iid, ar);

        assertEquals("REJECT", c.getData().getResult());
    }

    // ====== lifecycle 3：不良率 18% → 阻断 ======
            @Test
    @DisplayName("lifecycle 3：18% 不良率阻断入库 P1 修补 3")
    void testIntegration_DefectRateBlock() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(3L);
        ins.setResult("PENDING");
        ins.setSampleSize(50);
        ins.setSamplePass(41);
        ins.setSampleFail(9);
        ins.setDefectRate(new BigDecimal("18.00"));
        when(inspectionMapper.selectById(3L)).thenReturn(ins);

        Result<CrmPurchaseIncomingInspection> r = service.pass(3L, new SubmitResultRequest());
        assertEquals(40909, r.getCode());
    }

    // ====== 单号模板 ======
            @Test
    @DisplayName("AC-8.4.1：单号模板 PI{yyyyMMdd}{seq:4}")
    void testIntegration_PiNo() {
        when(inspectionMapper.selectByPoAndMaterial(any(), any())).thenReturn(null);
        CreateInspectionRequest cr = new CreateInspectionRequest();
        cr.setPoId(10L);
        cr.setPoNo("PO20260401-0001");
        cr.setMaterialId(1099L);
        cr.setInspectorId(601L);
        cr.setSampleSize(10);
        cr.setNotifyEmail("inspect@btsheng-163.com");
        Result<CrmPurchaseIncomingInspection> r = service.create(cr, 601L);
        assertTrue(r.getData().getInspectionNo().startsWith("PI"));
        assertEquals(15, r.getData().getInspectionNo().length());
    }

    // ====== 抽样 AQL 等级 ======
            @Test
    @DisplayName("AQL 等级 I/II/III 三档支持")
    void testIntegration_AqlLevels() {
        when(inspectionMapper.selectByPoAndMaterial(any(), any())).thenReturn(null);
        for (int i = 0; i < 3; i++) {
            CreateInspectionRequest cr = new CreateInspectionRequest();
            cr.setPoId(20L + i);
            cr.setPoNo("PO20260401-0001");
            cr.setMaterialId(2000L + i);
            cr.setInspectorId(601L);
            cr.setSampleSize(20);
            cr.setAqlLevel(i == 0 ? "I" : (i == 1 ? "II" : "III"));
            cr.setNotifyEmail("inspect@btsheng-163.com");
            Result<CrmPurchaseIncomingInspection> r = service.create(cr, 601L);
            assertEquals(0, r.getCode());
        }
    }

    // ====== 跨 1.32 PO 关联 ======
            @Test
    @DisplayName("跨 1.32：PO 关联（po_id + po_no 双绑）")
    void testIntegration_CrossPO() {
        when(inspectionMapper.selectByPoAndMaterial(any(), any())).thenReturn(null);
        CreateInspectionRequest cr = new CreateInspectionRequest();
        cr.setPoId(5L);
        cr.setPoNo("PO20260425-0009");
        cr.setMaterialId(1005L);
        cr.setInspectorId(601L);
        cr.setSampleSize(50);
        cr.setNotifyEmail("inspect@btsheng-163.com");
        Result<CrmPurchaseIncomingInspection> r = service.create(cr, 601L);
        assertEquals("PO20260425-0009", r.getData().getPoNo());
        assertEquals(5L, r.getData().getPoId());
    }

    // ====== 不良率计算精度 ======
            @Test
    @DisplayName("不良率：3/30 = 10.00% · 边界值")
    void testIntegration_DefectRateBoundary() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(99L);
        ins.setResult("PENDING");
        ins.setSampleSize(30);
        ins.setSamplePass(27);
        ins.setSampleFail(3);
        when(inspectionMapper.selectById(99L)).thenReturn(ins);

        SubmitResultRequest req = new SubmitResultRequest();
        req.setResult("PASS");
        Result<CrmPurchaseIncomingInspection> r = service.pass(99L, req);
        assertEquals(0, r.getCode());
        assertEquals(0, new BigDecimal("10.00").compareTo(ins.getDefectRate()));
    }

    // ====== 邮箱 AD-3 拦截 ======
            @Test
    @DisplayName("AD-3：163 之外所有邮箱（qq/126/gmail/outlook）拦截")
    void testIntegration_EmailAd3() {
        for (String bad : new String[]{"foo@qq.com", "bar@126.com", "baz@gmail.com", "qux@outlook.com"}) {
            CreateInspectionRequest cr = new CreateInspectionRequest();
            cr.setPoId(1L);
            cr.setPoNo("PO1");
            cr.setMaterialId(1L);
            cr.setInspectorId(601L);
            cr.setSampleSize(10);
            cr.setNotifyEmail(bad);
            Result<CrmPurchaseIncomingInspection> r = service.create(cr, 601L);
            assertEquals(40003, r.getCode(), "must reject: " + bad);
        }
    }

    // ====== 整单完成后再次 pass 拦截 ======
            @Test
    @DisplayName("已 PASS 状态再次 pass → 40903")
    void testIntegration_AlreadyFinalized_Pass() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(8L);
        ins.setResult("PASS");
        ins.setSampleSize(50);
        ins.setSamplePass(50);
        ins.setSampleFail(0);
        when(inspectionMapper.selectById(8L)).thenReturn(ins);
        Result<CrmPurchaseIncomingInspection> r = service.pass(8L, new SubmitResultRequest());
        assertEquals(40903, r.getCode());
    }

    // ====== list 含 items ======
            @Test
    @DisplayName("list：返回 inspection + items 完整结构")
    void testIntegration_List() {
        CrmPurchaseIncomingInspection ins1 = new CrmPurchaseIncomingInspection();
        ins1.setId(1L); ins1.setInspectionNo("PI20260612-0001"); ins1.setResult("PASS");
        CrmPurchaseIncomingInspection ins2 = new CrmPurchaseIncomingInspection();
        ins2.setId(2L); ins2.setInspectionNo("PI20260612-0002"); ins2.setResult("REJECT");
        when(inspectionMapper.selectAll()).thenReturn(List.of(ins1, ins2));
        when(itemMapper.selectByInspectionId(1L)).thenReturn(List.of());
        when(itemMapper.selectByInspectionId(2L)).thenReturn(List.of());

        Result<List<Map<String, Object>>> r = service.list();
        assertEquals(0, r.getCode());
        assertEquals(2, r.getData().size());
    }
}
