package com.btsheng.erp.production.outsource.incoming.integration;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.incoming.dto.AddDefectRequest;
import com.btsheng.erp.production.outsource.incoming.dto.IncomingInspectionRequest;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingDefect;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingInspection;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingItem;
import com.btsheng.erp.production.outsource.incoming.mapper.CrmOutsourceIncomingDefectMapper;
import com.btsheng.erp.production.outsource.incoming.mapper.CrmOutsourceIncomingInspectionMapper;
import com.btsheng.erp.production.outsource.incoming.mapper.CrmOutsourceIncomingItemMapper;
import com.btsheng.erp.production.outsource.incoming.service.OutsourceIncomingInspectionService;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.25 · 委外来料质检 集成测试（FR-6-5）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutsourceIncomingInspectionIntegrationTest {

    @Mock private CrmOutsourceIncomingInspectionMapper inspectionMapper;
    @Mock private CrmOutsourceIncomingItemMapper itemMapper;
    @Mock private CrmOutsourceIncomingDefectMapper defectMapper;
    @Mock private CrmOutsourceOrderMapper orderMapper;
    @Mock private ErpDocNoGenerator docNoGenerator;

    private OutsourceIncomingInspectionService service;

    @BeforeEach
    void setUp() {
        service = new OutsourceIncomingInspectionService(inspectionMapper, itemMapper, defectMapper, orderMapper, docNoGenerator);
        when(docNoGenerator.nextOutsourceInspectionNo())
                .thenReturn("OI20260612-0001", "OI20260612-0002", "OI20260612-0003", "OI20260612-0004");
        when(inspectionMapper.insert(any(CrmOutsourceIncomingInspection.class))).thenAnswer(inv -> {
            CrmOutsourceIncomingInspection i = inv.getArgument(0);
            i.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(itemMapper.insert(any(CrmOutsourceIncomingItem.class))).thenAnswer(inv -> {
            CrmOutsourceIncomingItem i = inv.getArgument(0);
            i.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(defectMapper.insert(any(CrmOutsourceIncomingDefect.class))).thenAnswer(inv -> {
            CrmOutsourceIncomingDefect d = inv.getArgument(0);
            d.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(inspectionMapper.updateById(any(CrmOutsourceIncomingInspection.class))).thenReturn(1);
    }

    private CrmOutsourceOrder mockOrder(Long id) {
        CrmOutsourceOrder o = new CrmOutsourceOrder();
        o.setId(id);
        o.setOutsourceNo("WW" + id);
        o.setSupplierId(101L);
        o.setSupplierName("Test");
        o.setMaterialCode("ZZ-0001");
        o.setStatus("COMPLETED");
        return o;
    }

    private IncomingInspectionRequest.IncomingItemDto mockItem(String name, int passed) {
        IncomingInspectionRequest.IncomingItemDto i = new IncomingInspectionRequest.IncomingItemDto();
        i.setItemName(name);
        i.setPassed(passed);
        return i;
    }

    // ====== 完整 lifecycle 1：创建 → 检验 → PASSED ======
            @Test
    @DisplayName("集成 lifecycle 1：创建 → 添加缺陷 → PASSED")
    void testIntegration_Lifecycle_Pass() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L));

        IncomingInspectionRequest req = new IncomingInspectionRequest();
        req.setOutsourceId(1L);
        req.setInspectQty(10);
        req.setNotifyEmail("qa@163.com");
        req.setItems(Arrays.asList(mockItem("外观", 1), mockItem("硬度", 1)));

        Result<CrmOutsourceIncomingInspection> c = service.createInspection(req, 301L);
        assertEquals(0, c.getCode());
        Long insId = c.getData().getId();

        // 模拟添加一个 MINOR 缺陷
            CrmOutsourceIncomingInspection refreshed = new CrmOutsourceIncomingInspection();
        refreshed.setId(insId);
        refreshed.setInspectQty(10);
        refreshed.setFailedQty(0);
        refreshed.setResult("DRAFT");
        when(inspectionMapper.selectById(insId)).thenReturn(refreshed);

        AddDefectRequest dr = new AddDefectRequest();
        dr.setInspectionId(insId);
        dr.setDefectType("毛刺");
        dr.setSeverity("MINOR");
        dr.setQty(1);
        Result<CrmOutsourceIncomingDefect> ad = service.addDefect(dr, 301L);
        assertEquals(0, ad.getCode());

        // 通过
            when(defectMapper.countCriticalByInspectionId(insId)).thenReturn(0);
        refreshed.setFailedQty(1);
        Result<CrmOutsourceIncomingInspection> p = service.pass(insId, 301L);
        assertEquals(0, p.getCode());
        assertEquals("PASSED", p.getData().getResult());
    }

    // ====== 完整 lifecycle 2：CRITICAL → 拒绝 PASS → reject ======
            @Test
    @DisplayName("集成 lifecycle 2：CRITICAL 缺陷 → 拒绝 pass → reject 触发返修")
    void testIntegration_Lifecycle_CriticalReject() {
        when(orderMapper.selectById(2L)).thenReturn(mockOrder(2L));
        IncomingInspectionRequest req = new IncomingInspectionRequest();
        req.setOutsourceId(2L);
        req.setInspectQty(10);
        req.setItems(Arrays.asList(mockItem("外观", 0)));
        Result<CrmOutsourceIncomingInspection> c = service.createInspection(req, 301L);
        Long insId = c.getData().getId();

        CrmOutsourceIncomingInspection ins = new CrmOutsourceIncomingInspection();
        ins.setId(insId);
        ins.setOutsourceId(2L);
        ins.setOutsourceNo("WW2");
        ins.setInspectQty(10);
        ins.setResult("DRAFT");
        when(inspectionMapper.selectById(insId)).thenReturn(ins);

        // 添加 CRITICAL
            AddDefectRequest cr = new AddDefectRequest();
        cr.setInspectionId(insId);
        cr.setDefectType("裂纹");
        cr.setSeverity("CRITICAL");
        cr.setQty(1);
        Result<CrmOutsourceIncomingDefect> ad = service.addDefect(cr, 301L);
        assertEquals(0, ad.getCode());

        // 尝试 PASS 被拒
            when(defectMapper.countCriticalByInspectionId(insId)).thenReturn(1);
        Result<CrmOutsourceIncomingInspection> p = service.pass(insId, 301L);
        assertEquals(40903, p.getCode());

        // reject 触发返修
            Result<Map<String, Object>> r = service.reject(insId, "裂纹", 301L);
        assertEquals(0, r.getCode());
        assertEquals(true, r.getData().get("reworkTriggered"));
    }

    // ====== CONDITIONAL：不良率 ≤ 10% ======
            @Test
    @DisplayName("集成 CONDITIONAL：不良率 5% 可 CONDITIONAL")
    void testIntegration_Conditional_OK() {
        CrmOutsourceIncomingInspection ins = new CrmOutsourceIncomingInspection();
        ins.setId(100L);
        ins.setInspectQty(20);
        ins.setDefectRate(new BigDecimal("5.00"));
        ins.setResult("DRAFT");
        when(inspectionMapper.selectById(100L)).thenReturn(ins);

        Result<CrmOutsourceIncomingInspection> r = service.conditional(100L, 301L);
        assertEquals(0, r.getCode());
        assertEquals("CONDITIONAL", r.getData().getResult());
    }

    @Test
    @DisplayName("集成 CONDITIONAL：不良率 15% 不可 CONDITIONAL")
    void testIntegration_Conditional_Fail() {
        CrmOutsourceIncomingInspection ins = new CrmOutsourceIncomingInspection();
        ins.setId(101L);
        ins.setDefectRate(new BigDecimal("15.00"));
        when(inspectionMapper.selectById(101L)).thenReturn(ins);

        Result<CrmOutsourceIncomingInspection> r = service.conditional(101L, 301L);
        assertEquals(40903, r.getCode());
    }

    // ====== 跨模块 1.18：委外单联动 ======
            @Test
    @DisplayName("跨模块 1.18：委外单不存在 → 40404")
    void testIntegration_Cross_18_OrderNotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);

        IncomingInspectionRequest req = new IncomingInspectionRequest();
        req.setOutsourceId(999L);
        req.setInspectQty(10);
        req.setItems(Arrays.asList(mockItem("外观", 1)));

        Result<CrmOutsourceIncomingInspection> r = service.createInspection(req, 301L);
        assertEquals(40404, r.getCode());
    }

    // ====== 审计留痕 ======
            @Test
    @DisplayName("审计留痕：createInspection 写 1 inspection + N items")
    void testIntegration_Audit() {
        when(orderMapper.selectById(3L)).thenReturn(mockOrder(3L));
        IncomingInspectionRequest req = new IncomingInspectionRequest();
        req.setOutsourceId(3L);
        req.setInspectQty(10);
        req.setItems(Arrays.asList(mockItem("外观", 1), mockItem("硬度", 1), mockItem("尺寸", 1)));

        service.createInspection(req, 301L);

        verify(inspectionMapper, times(1)).insert(any(CrmOutsourceIncomingInspection.class));
        verify(itemMapper, times(3)).insert(any(CrmOutsourceIncomingItem.class));
    }

    // ====== 不良率自动累加 ======
            @Test
    @DisplayName("集成：添加 2 个缺陷后失败率自动累加")
    void testIntegration_DefectRateAccumulate() {
        CrmOutsourceIncomingInspection ins = new CrmOutsourceIncomingInspection();
        ins.setId(200L);
        ins.setInspectQty(10);
        ins.setFailedQty(1);
        ins.setResult("DRAFT");
        when(inspectionMapper.selectById(200L)).thenReturn(ins);

        AddDefectRequest req = new AddDefectRequest();
        req.setInspectionId(200L);
        req.setDefectType("x");
        req.setSeverity("MINOR");
        req.setQty(1);
        service.addDefect(req, 301L);

        verify(inspectionMapper, atLeastOnce()).updateById(argThat((CrmOutsourceIncomingInspection i) ->
                i.getFailedQty() != null && i.getFailedQty() == 2
                && i.getDefectRate() != null && i.getDefectRate().compareTo(new BigDecimal("20.00")) == 0));
    }

    // ====== 单一 163 邮箱 AD-3 强约束 ======
            @Test
    @DisplayName("AD-3：qq.com 邮箱拒绝")
    void testIntegration_Email_QQ() {
        IncomingInspectionRequest req = new IncomingInspectionRequest();
        req.setOutsourceId(4L);
        req.setInspectQty(10);
        req.setNotifyEmail("qa@qq.com");
        req.setItems(Arrays.asList(mockItem("外观", 1)));

        Result<CrmOutsourceIncomingInspection> r = service.createInspection(req, 301L);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("AD-3：gmail.com 邮箱拒绝")
    void testIntegration_Email_Gmail() {
        IncomingInspectionRequest req = new IncomingInspectionRequest();
        req.setOutsourceId(5L);
        req.setInspectQty(10);
        req.setNotifyEmail("qa@gmail.com");
        req.setItems(Arrays.asList(mockItem("外观", 1)));

        Result<CrmOutsourceIncomingInspection> r = service.createInspection(req, 301L);
        assertEquals(40001, r.getCode());
    }

    // ====== 跨模块 1.23：reject → rework 触发信号 ======
            @Test
    @DisplayName("跨模块 1.23：reject 携带返修原因给 rework 模块")
    void testIntegration_Cross_23_ReworkTrigger() {
        CrmOutsourceIncomingInspection ins = new CrmOutsourceIncomingInspection();
        ins.setId(300L);
        ins.setOutsourceId(50L);
        ins.setOutsourceNo("WW50");
        ins.setInspectQty(10);
        ins.setResult("DRAFT");
        when(inspectionMapper.selectById(300L)).thenReturn(ins);

        Result<Map<String, Object>> r = service.reject(300L, "硬度不足", 301L);
        assertEquals(0, r.getCode());
        assertEquals("硬度不足", r.getData().get("reworkReason"));
        assertEquals(50L, r.getData().get("outsourceId"));
    }
}
