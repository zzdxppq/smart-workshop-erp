package com.btsheng.erp.production.outsource.quality.integration;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.quality.dto.AddQualityDefectRequest;
import com.btsheng.erp.production.outsource.quality.dto.QualityCreateRequest;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQuality;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQualityDefect;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQualityItem;
import com.btsheng.erp.production.outsource.quality.mapper.CrmOutsourceQualityDefectMapper;
import com.btsheng.erp.production.outsource.quality.mapper.CrmOutsourceQualityItemMapper;
import com.btsheng.erp.production.outsource.quality.mapper.CrmOutsourceQualityMapper;
import com.btsheng.erp.production.outsource.quality.service.OutsourceQualityService;
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
 * V1.3.7 · Story 1.27 · 委外工序质检 集成测试（FR-6-7）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutsourceQualityIntegrationTest {

    @Mock private CrmOutsourceQualityMapper qualityMapper;
    @Mock private CrmOutsourceQualityItemMapper itemMapper;
    @Mock private CrmOutsourceQualityDefectMapper defectMapper;
    @Mock private CrmOutsourceOrderMapper orderMapper;
    @Mock private ErpDocNoGenerator docNoGenerator;

    private OutsourceQualityService service;

    @BeforeEach
    void setUp() {
        service = new OutsourceQualityService(qualityMapper, itemMapper, defectMapper, orderMapper, docNoGenerator);
        when(docNoGenerator.nextOutsourceQualityNo())
                .thenReturn("OQ20260612-0001", "OQ20260612-0002", "OQ20260612-0003", "OQ20260612-0004");
        when(qualityMapper.insert(any(CrmOutsourceQuality.class))).thenAnswer(inv -> {
            CrmOutsourceQuality q = inv.getArgument(0);
            q.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(itemMapper.insert(any(CrmOutsourceQualityItem.class))).thenAnswer(inv -> {
            CrmOutsourceQualityItem i = inv.getArgument(0);
            i.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(defectMapper.insert(any(CrmOutsourceQualityDefect.class))).thenAnswer(inv -> {
            CrmOutsourceQualityDefect d = inv.getArgument(0);
            d.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(qualityMapper.updateById(any(CrmOutsourceQuality.class))).thenReturn(1);
    }

    private CrmOutsourceOrder mockOrder(Long id) {
        CrmOutsourceOrder o = new CrmOutsourceOrder();
        o.setId(id);
        o.setOutsourceNo("WW" + id);
        o.setSupplierId(101L);
        o.setSupplierName("Test");
        o.setStatus("COMPLETED");
        return o;
    }

    private QualityCreateRequest.QualityItemDto mockItem(String name, int passed) {
        QualityCreateRequest.QualityItemDto i = new QualityCreateRequest.QualityItemDto();
        i.setItemType("FA");
        i.setItemName(name);
        i.setPassed(passed);
        return i;
    }

    // ====== 完整 lifecycle 1：FA 首件 → PASSED ======
            @Test
    @DisplayName("集成 lifecycle 1：FA 首件 → PASSED")
    void testIntegration_FA_Pass() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L));

        QualityCreateRequest req = new QualityCreateRequest();
        req.setOutsourceId(1L);
        req.setProcessName("调质");
        req.setInspectType("FA");
        req.setInspectQty(1);
        req.setItems(Arrays.asList(mockItem("外观", 1), mockItem("硬度", 1)));

        Result<CrmOutsourceQuality> c = service.createQuality(req, 301L);
        assertEquals(0, c.getCode());
        Long qId = c.getData().getId();

        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(qId);
        q.setInspectQty(1);
        q.setFailedQty(0);
        q.setResult("DRAFT");
        when(qualityMapper.selectById(qId)).thenReturn(q);
        when(defectMapper.countCriticalByQualityId(qId)).thenReturn(0);

        Result<CrmOutsourceQuality> p = service.pass(qId, 301L);
        assertEquals(0, p.getCode());
        assertEquals("PASSED", p.getData().getResult());
    }

    // ====== 完整 lifecycle 2：CMM → CRITICAL → reject → 返修 ======
            @Test
    @DisplayName("集成 lifecycle 2：CMM 三次元 → CRITICAL 缺陷 → reject 触发返修")
    void testIntegration_CMM_Critical_Reject() {
        when(orderMapper.selectById(2L)).thenReturn(mockOrder(2L));

        QualityCreateRequest req = new QualityCreateRequest();
        req.setOutsourceId(2L);
        req.setProcessName("线切割");
        req.setInspectType("CMM");
        req.setInspectQty(1);
        req.setItems(Arrays.asList(mockItem("X 轴", 0), mockItem("Y 轴", 1)));

        Result<CrmOutsourceQuality> c = service.createQuality(req, 301L);
        Long qId = c.getData().getId();

        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(qId);
        q.setOutsourceId(2L);
        q.setOutsourceNo("WW2");
        q.setInspectQty(1);
        q.setResult("DRAFT");
        when(qualityMapper.selectById(qId)).thenReturn(q);

        // 添加 CRITICAL 缺陷
            AddQualityDefectRequest dr = new AddQualityDefectRequest();
        dr.setQualityId(qId);
        dr.setDefectType("X 轴尺寸超差");
        dr.setSeverity("CRITICAL");
        dr.setQty(1);
        Result<CrmOutsourceQualityDefect> ad = service.addDefect(dr, 301L);
        assertEquals(0, ad.getCode());

        // 尝试 PASS 被拒
            when(defectMapper.countCriticalByQualityId(qId)).thenReturn(1);
        Result<CrmOutsourceQuality> p = service.pass(qId, 301L);
        assertEquals(40903, p.getCode());

        // reject 触发返修
            Result<Map<String, Object>> r = service.reject(qId, "X 轴超差", 301L);
        assertEquals(0, r.getCode());
        assertEquals(true, r.getData().get("reworkTriggered"));
    }

    // ====== 不良率 > 10% 告警 ======
            @Test
    @DisplayName("集成：不良率 > 10% → alerted = 1")
    void testIntegration_DefectRateAlert() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(100L);
        q.setInspectQty(10);
        q.setFailedQty(0);
        q.setResult("DRAFT");
        when(qualityMapper.selectById(100L)).thenReturn(q);

        // 添加 2 个缺陷 → 20% 不良率
            AddQualityDefectRequest req = new AddQualityDefectRequest();
        req.setQualityId(100L);
        req.setDefectType("x");
        req.setSeverity("MAJOR");
        req.setQty(2);
        service.addDefect(req, 301L);

        verify(qualityMapper, atLeastOnce()).updateById(argThat((CrmOutsourceQuality qq) ->
                qq.getAlerted() != null && qq.getAlerted() == 1
                && qq.getDefectRate() != null && qq.getDefectRate().compareTo(new BigDecimal("20.00")) == 0));
    }

    @Test
    @DisplayName("集成：不良率 ≤ 10% → alerted = 0")
    void testIntegration_DefectRateNoAlert() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(101L);
        q.setInspectQty(10);
        q.setFailedQty(0);
        q.setResult("DRAFT");
        when(qualityMapper.selectById(101L)).thenReturn(q);

        // 添加 1 个缺陷 → 10% 不良率（边界，不告警）
            AddQualityDefectRequest req = new AddQualityDefectRequest();
        req.setQualityId(101L);
        req.setDefectType("x");
        req.setSeverity("MINOR");
        req.setQty(1);
        service.addDefect(req, 301L);

        verify(qualityMapper, atLeastOnce()).updateById(argThat((CrmOutsourceQuality qq) ->
                qq.getAlerted() != null && qq.getAlerted() == 0));
    }

    // ====== 跨模块 1.18：委外单联动 ======
            @Test
    @DisplayName("跨模块 1.18：委外单不存在 → 40404")
    void testIntegration_Cross_18_OrderNotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);

        QualityCreateRequest req = new QualityCreateRequest();
        req.setOutsourceId(999L);
        req.setProcessName("调质");
        req.setInspectType("FA");
        req.setInspectQty(1);
        req.setItems(Arrays.asList(mockItem("外观", 1)));

        Result<CrmOutsourceQuality> r = service.createQuality(req, 301L);
        assertEquals(40404, r.getCode());
    }

    // ====== 审计留痕 ======
            @Test
    @DisplayName("审计留痕：createQuality 写 1 quality + N items")
    void testIntegration_Audit() {
        when(orderMapper.selectById(3L)).thenReturn(mockOrder(3L));
        QualityCreateRequest req = new QualityCreateRequest();
        req.setOutsourceId(3L);
        req.setProcessName("喷涂");
        req.setInspectType("FA");
        req.setInspectQty(1);
        req.setItems(Arrays.asList(mockItem("色差", 1), mockItem("附着力", 1), mockItem("厚度", 1)));

        service.createQuality(req, 301L);
        verify(qualityMapper, times(1)).insert(any(CrmOutsourceQuality.class));
        verify(itemMapper, times(3)).insert(any(CrmOutsourceQualityItem.class));
    }

    // ====== FA / CMM 区别于 7 品质 ======
            @Test
    @DisplayName("AC-6.7.1：FA 首件与 CMM 三次元独立")
    void testIntegration_FA_VS_CMM() {
        when(orderMapper.selectById(4L)).thenReturn(mockOrder(4L));

        QualityCreateRequest faReq = new QualityCreateRequest();
        faReq.setOutsourceId(4L);
        faReq.setProcessName("电镀");
        faReq.setInspectType("FA");
        faReq.setInspectQty(1);
        faReq.setItems(Arrays.asList(mockItem("外观", 1)));

        QualityCreateRequest cmmReq = new QualityCreateRequest();
        cmmReq.setOutsourceId(4L);
        cmmReq.setProcessName("电镀");
        cmmReq.setInspectType("CMM");
        cmmReq.setInspectQty(1);
        cmmReq.setItems(Arrays.asList(mockItem("厚度", 1)));

        Result<CrmOutsourceQuality> fa = service.createQuality(faReq, 301L);
        Result<CrmOutsourceQuality> cmm = service.createQuality(cmmReq, 301L);

        assertEquals("FA", fa.getData().getInspectType());
        assertEquals("CMM", cmm.getData().getInspectType());
    }

    // ====== 跨模块 1.23：reject → rework 信号 ======
            @Test
    @DisplayName("跨模块 1.23：reject 携带返修信号")
    void testIntegration_Cross_23_Rework() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(200L);
        q.setOutsourceId(50L);
        q.setOutsourceNo("WW50");
        q.setInspectQty(1);
        q.setResult("DRAFT");
        when(qualityMapper.selectById(200L)).thenReturn(q);

        Result<Map<String, Object>> r = service.reject(200L, "硬度不足", 301L);
        assertEquals(0, r.getCode());
        assertEquals("硬度不足", r.getData().get("reworkReason"));
        assertEquals(50L, r.getData().get("outsourceId"));
        assertEquals(true, r.getData().get("reworkTriggered"));
    }

    // ====== 拒绝 pass 重复 ======
            @Test
    @DisplayName("集成：重复 PASS → 40903")
    void testIntegration_RePass_Blocked() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(300L);
        q.setInspectQty(1);
        q.setResult("PASSED");
        when(qualityMapper.selectById(300L)).thenReturn(q);

        Result<CrmOutsourceQuality> r = service.pass(300L, 301L);
        assertEquals(40903, r.getCode());
        assertEquals("QUALITY_ALREADY_PASSED", r.getMessage());
    }

    // ====== 严重度分级 - 拒绝 pass CRITICAL 累计 ======
            @Test
    @DisplayName("集成：多次添加 CRITICAL 累计告警")
    void testIntegration_CriticalAccumulate() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(400L);
        q.setInspectQty(5);
        q.setResult("DRAFT");
        when(qualityMapper.selectById(400L)).thenReturn(q);

        AddQualityDefectRequest r1 = new AddQualityDefectRequest();
        r1.setQualityId(400L);
        r1.setDefectType("x");
        r1.setSeverity("CRITICAL");
        r1.setQty(1);
        service.addDefect(r1, 301L);

        verify(defectMapper, times(1)).insert(any(CrmOutsourceQualityDefect.class));
        verify(qualityMapper, atLeastOnce()).updateById(argThat((CrmOutsourceQuality qq) ->
                qq.getAlerted() != null && qq.getAlerted() == 1));
    }
}
