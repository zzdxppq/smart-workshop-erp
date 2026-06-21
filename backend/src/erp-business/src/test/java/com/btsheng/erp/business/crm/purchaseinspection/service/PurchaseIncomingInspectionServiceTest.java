package com.btsheng.erp.business.crm.purchaseinspection.service;

import com.btsheng.erp.business.crm.purchaseinspection.dto.AddItemRequest;
import com.btsheng.erp.business.crm.purchaseinspection.dto.CreateInspectionRequest;
import com.btsheng.erp.business.crm.purchaseinspection.dto.SubmitResultRequest;
import com.btsheng.erp.business.crm.purchaseinspection.entity.CrmPurchaseIncomingInspection;
import com.btsheng.erp.business.crm.purchaseinspection.entity.CrmPurchaseIncomingItem;
import com.btsheng.erp.business.crm.purchaseinspection.mapper.CrmPurchaseIncomingInspectionMapper;
import com.btsheng.erp.business.crm.purchaseinspection.mapper.CrmPurchaseIncomingItemMapper;
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
 * V1.3.7 · Story 1.35 · 采购·来料质检 Service 单元测试（FR-8-4）
 * 14 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseIncomingInspectionServiceTest {

    @Mock private CrmPurchaseIncomingInspectionMapper inspectionMapper;
    @Mock private CrmPurchaseIncomingItemMapper itemMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private PurchaseIncomingInspectionService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseIncomingInspectionService(inspectionMapper, itemMapper, docNoGenerator);
        when(docNoGenerator.nextPurchaseIncomingInspectionNo())
                .thenReturn("PI20260612-0001", "PI20260612-0002", "PI20260612-0003", "PI20260612-0004");
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

    private CreateInspectionRequest buildValid() {
        CreateInspectionRequest r = new CreateInspectionRequest();
        r.setPoId(1L);
        r.setPoNo("PO20260401-0001");
        r.setMaterialId(1001L);
        r.setMaterialCode("M-AL6061-PT");
        r.setInspectorId(601L);
        r.setInspectorName("李工");
        r.setSampleSize(50);
        r.setAqlLevel("II");
        r.setNotifyEmail("inspect@btsheng-163.com");
        return r;
    }

    // ====== create 5 测例 ======
            @Test
    @DisplayName("create happy path · 单号 PI 前缀")
    void testCreate_OK() {
        when(inspectionMapper.selectByPoAndMaterial(any(), any())).thenReturn(null);
        Result<CrmPurchaseIncomingInspection> r = service.create(buildValid(), 601L);
        assertEquals(0, r.getCode());
        assertEquals("PI20260612-0001", r.getData().getInspectionNo());
        assertEquals("PENDING", r.getData().getResult());
        assertEquals("inspect@btsheng-163.com", r.getData().getNotifyEmail());
    }

    @Test
    @DisplayName("P1 修补 1：邮箱非 163 → 40003 AD-3")
    void testCreate_EmailNot163() {
        CreateInspectionRequest r = buildValid();
        r.setNotifyEmail("inspect@qq.com");
        Result<CrmPurchaseIncomingInspection> res = service.create(r, 601L);
        assertEquals(40003, res.getCode());
        assertEquals("EMAIL_NOT_163_AD3", res.getMessage());
    }

    @Test
    @DisplayName("P1 修补 1：null 邮箱自动填充 163 邮箱")
    void testCreate_EmailNull_Default163() {
        when(inspectionMapper.selectByPoAndMaterial(any(), any())).thenReturn(null);
        CreateInspectionRequest r = buildValid();
        r.setNotifyEmail(null);
        Result<CrmPurchaseIncomingInspection> res = service.create(r, 601L);
        assertEquals(0, res.getCode());
        assertEquals("inspect@btsheng-163.com", res.getData().getNotifyEmail());
    }

    @Test
    @DisplayName("P1 修补 4：(po_id, material_id) 重复 → 40902")
    void testCreate_Duplicate() {
        CrmPurchaseIncomingInspection existed = new CrmPurchaseIncomingInspection();
        existed.setId(99L);
        when(inspectionMapper.selectByPoAndMaterial(1L, 1001L)).thenReturn(existed);
        Result<CrmPurchaseIncomingInspection> r = service.create(buildValid(), 601L);
        assertEquals(40902, r.getCode());
    }

    @Test
    @DisplayName("create 缺 inspectorId → 40001")
    void testCreate_NoInspector() {
        CreateInspectionRequest r = buildValid();
        r.setInspectorId(null);
        Result<CrmPurchaseIncomingInspection> res = service.create(r, 601L);
        assertEquals(40001, res.getCode());
    }

    // ====== addItem 4 测例 ======
            @Test
    @DisplayName("addItem happy path · 累加 pass/fail · 不良率")
    void testAddItem_OK() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(1L);
        ins.setResult("PENDING");
        ins.setSampleSize(50);
        ins.setSamplePass(0);
        ins.setSampleFail(0);
        when(inspectionMapper.selectById(1L)).thenReturn(ins);
        when(itemMapper.selectByInspectionId(1L)).thenReturn(new ArrayList<>());

        AddItemRequest req = new AddItemRequest();
        req.setCheckItem("外观");
        req.setSampleQty(20);
        req.setPassQty(20);
        req.setFailQty(0);
        Result<CrmPurchaseIncomingItem> r = service.addItem(1L, req);
        assertEquals(0, r.getCode());
        assertEquals("PASS", r.getData().getResult());
        assertEquals(0, new BigDecimal("0.00").compareTo(ins.getDefectRate()));
    }

    @Test
    @DisplayName("P1 修补 2：AQL 等级 II 字段保留")
    void testAddItem_Aql() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(1L);
        ins.setResult("PENDING");
        ins.setAqlLevel("II");
        ins.setSampleSize(50);
        when(inspectionMapper.selectById(1L)).thenReturn(ins);
        when(itemMapper.selectByInspectionId(1L)).thenReturn(new ArrayList<>());

        AddItemRequest req = new AddItemRequest();
        req.setCheckItem("尺寸");
        req.setSampleQty(20);
        req.setPassQty(19);
        req.setFailQty(1);
        req.setIsCritical(0);
        Result<CrmPurchaseIncomingItem> r = service.addItem(1L, req);
        assertEquals(0, r.getCode());
        assertEquals("II", ins.getAqlLevel());
    }

    @Test
    @DisplayName("关键项 FAIL → 整单 REJECT（1 票否决）")
    void testAddItem_CriticalFail_Reject() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(1L);
        ins.setResult("PENDING");
        ins.setSampleSize(50);
        ins.setSamplePass(0);
        ins.setSampleFail(0);
        when(inspectionMapper.selectById(1L)).thenReturn(ins);
        when(itemMapper.selectByInspectionId(1L)).thenReturn(new ArrayList<>());

        AddItemRequest req = new AddItemRequest();
        req.setCheckItem("尺寸");
        req.setSampleQty(20);
        req.setPassQty(15);
        req.setFailQty(5);
        req.setIsCritical(1);
        Result<CrmPurchaseIncomingItem> r = service.addItem(1L, req);
        assertEquals(0, r.getCode());
        assertEquals("FAIL", r.getData().getResult());
        assertEquals("REJECT", ins.getResult());
    }

    @Test
    @DisplayName("addItem 已 REJECT 再添加 → 40903")
    void testAddItem_AlreadyFinalized() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(1L);
        ins.setResult("REJECT");
        when(inspectionMapper.selectById(1L)).thenReturn(ins);
        AddItemRequest req = new AddItemRequest();
        req.setCheckItem("外观");
        req.setSampleQty(10);
        Result<CrmPurchaseIncomingItem> r = service.addItem(1L, req);
        assertEquals(40903, r.getCode());
    }

    // ====== pass 2 测例 ======
            @Test
    @DisplayName("P1 修补 3：不良率 > 10% → 阻断 40909")
    void testPass_DefectRateOver_Block() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(1L);
        ins.setResult("PENDING");
        ins.setSampleSize(50);
        ins.setSamplePass(41);
        ins.setSampleFail(9);
        ins.setDefectRate(new BigDecimal("18.00"));
        when(inspectionMapper.selectById(1L)).thenReturn(ins);

        SubmitResultRequest req = new SubmitResultRequest();
        req.setResult("PASS");
        Result<CrmPurchaseIncomingInspection> r = service.pass(1L, req);
        assertEquals(40909, r.getCode());
        assertEquals("DEFECT_RATE_OVER_10_BLOCK", r.getMessage());
    }

    @Test
    @DisplayName("pass · 不良率 3% → PASS 成功")
    void testPass_OK() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(1L);
        ins.setResult("PENDING");
        ins.setSampleSize(50);
        ins.setSamplePass(48);
        ins.setSampleFail(2);
        when(inspectionMapper.selectById(1L)).thenReturn(ins);

        SubmitResultRequest req = new SubmitResultRequest();
        req.setResult("PASS");
        Result<CrmPurchaseIncomingInspection> r = service.pass(1L, req);
        assertEquals(0, r.getCode());
        assertEquals("PASS", ins.getResult());
        assertEquals(0, new BigDecimal("4.00").compareTo(ins.getDefectRate()));
    }

    // ====== reject + list 3 测例 ======
            @Test
    @DisplayName("reject · 直接 REJECT")
    void testReject_OK() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(1L);
        ins.setResult("PENDING");
        ins.setSampleSize(50);
        ins.setSamplePass(20);
        ins.setSampleFail(30);
        when(inspectionMapper.selectById(1L)).thenReturn(ins);
        Result<CrmPurchaseIncomingInspection> r = service.reject(1L, new SubmitResultRequest());
        assertEquals(0, r.getCode());
        assertEquals("REJECT", ins.getResult());
    }

    @Test
    @DisplayName("reject · 多次 pass/reject → 40903")
    void testReject_AlreadyFinalized() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(1L);
        ins.setResult("PASS");
        when(inspectionMapper.selectById(1L)).thenReturn(ins);
        Result<CrmPurchaseIncomingInspection> r = service.reject(1L, new SubmitResultRequest());
        assertEquals(40903, r.getCode());
    }

    @Test
    @DisplayName("list · 含 items")
    void testList_OK() {
        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setId(1L);
        ins.setInspectionNo("PI20260612-0001");
        ins.setResult("PASS");
        when(inspectionMapper.selectAll()).thenReturn(List.of(ins));

        CrmPurchaseIncomingItem it = new CrmPurchaseIncomingItem();
        it.setId(11L);
        it.setInspectionId(1L);
        it.setCheckItem("外观");
        when(itemMapper.selectByInspectionId(1L)).thenReturn(List.of(it));

        Result<List<Map<String, Object>>> r = service.list();
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
        Map<String, Object> entry = r.getData().get(0);
        assertEquals(ins, entry.get("inspection"));
        assertEquals(1, ((List<?>) entry.get("items")).size());
    }
}
