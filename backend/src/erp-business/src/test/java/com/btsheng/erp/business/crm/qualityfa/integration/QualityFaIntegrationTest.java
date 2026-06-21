package com.btsheng.erp.business.crm.qualityfa.integration;

import com.btsheng.erp.business.crm.qualityfa.dto.FaCreateRequest;
import com.btsheng.erp.business.crm.qualityfa.entity.CrmQualityFa;
import com.btsheng.erp.business.crm.qualityfa.entity.CrmQualityFaItem;
import com.btsheng.erp.business.crm.qualityfa.mapper.CrmQualityFaItemMapper;
import com.btsheng.erp.business.crm.qualityfa.mapper.CrmQualityFaMapper;
import com.btsheng.erp.business.crm.qualityfa.service.QualityFaService;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.29 · 品质·FA 首件 集成测试（FR-7-2）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QualityFaIntegrationTest {

    @Mock private CrmQualityFaMapper faMapper;
    @Mock private CrmQualityFaItemMapper itemMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private QualityFaService service;

    @BeforeEach
    void setUp() {
        service = new QualityFaService(faMapper, itemMapper, docNoGenerator);
        when(docNoGenerator.nextQualityFaNo())
                .thenReturn("QF20260612-0001", "QF20260612-0002", "QF20260612-0003", "QF20260612-0004", "QF20260612-0005");
        when(faMapper.insert(any(CrmQualityFa.class))).thenAnswer(inv -> {
            CrmQualityFa f = inv.getArgument(0);
            f.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(itemMapper.insert(any(CrmQualityFaItem.class))).thenAnswer(inv -> {
            CrmQualityFaItem i = inv.getArgument(0);
            i.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(faMapper.updateById(any(CrmQualityFa.class))).thenReturn(1);
    }

    private FaCreateRequest.FaItemDto item(String dim, String name, int passed) {
        FaCreateRequest.FaItemDto i = new FaCreateRequest.FaItemDto();
        i.setDimension(dim);
        i.setItemName(name);
        i.setPassed(passed);
        return i;
    }

    private FaCreateRequest buildReq() {
        FaCreateRequest req = new FaCreateRequest();
        req.setWorkOrderId(1L);
        req.setWorkOrderNo("GD20260608-0001");
        req.setProcessId(10L);
        req.setProcessName("粗车");
        req.setItems(Arrays.asList(
                item("尺寸", "外径 φ50", 1),
                item("粗糙度", "Ra 1.6", 1),
                item("外观", "无缺陷", 1)));
        return req;
    }

    // ====== 完整 lifecycle 1：FA → PASSED + PDF 报告 ======
            @Test
    @DisplayName("集成 lifecycle 1：FA 创建 → 全 PASS → PASSED + PDF")
    void testIntegration_FA_Pass() {
        Result<CrmQualityFa> c = service.createFa(buildReq(), 401L);
        assertEquals(0, c.getCode());
        Long id = c.getData().getId();

        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(id);
        fa.setFaNo("QF20260612-0001");
        fa.setResult("DRAFT");
        when(faMapper.selectById(id)).thenReturn(fa);
        CrmQualityFaItem item = new CrmQualityFaItem();
        item.setPassed(1);
        when(itemMapper.selectByFaId(id)).thenReturn(List.of(item));

        Result<CrmQualityFa> p = service.pass(id, 401L);
        assertEquals(0, p.getCode());
        assertEquals("PASSED", p.getData().getResult());
        assertNotNull(p.getData().getPdfUrl());
    }

    // ====== 完整 lifecycle 2：FA → FAILED → 锁定工序阻断生产 ======
            @Test
    @DisplayName("集成 lifecycle 2：FA 有 FAILED → 阻断 PASS → reject 锁定工序")
    void testIntegration_FA_Reject_LockProcess() {
        FaCreateRequest req = buildReq();
        req.setItems(Arrays.asList(
                item("尺寸", "外径 φ50", 0),
                item("粗糙度", "Ra 1.6", 1)));
        Result<CrmQualityFa> c = service.createFa(req, 401L);
        Long id = c.getData().getId();

        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(id);
        fa.setWorkOrderId(1L);
        fa.setProcessId(10L);
        fa.setResult("DRAFT");
        when(faMapper.selectById(id)).thenReturn(fa);
        CrmQualityFaItem item = new CrmQualityFaItem();
        item.setPassed(0);
        when(itemMapper.selectByFaId(id)).thenReturn(List.of(item));

        // 阻断 PASS
            Result<CrmQualityFa> p = service.pass(id, 401L);
        assertEquals(40903, p.getCode());

        // reject 锁定工序
            Result<Map<String, Object>> r = service.reject(id, "尺寸超差", 401L);
        assertEquals(0, r.getCode());
        assertEquals(true, r.getData().get("processLocked"));
        verify(faMapper, atLeastOnce()).updateById(argThat((CrmQualityFa ff) ->
                ff.getLocked() != null && ff.getLocked() == 1));
    }

    // ====== 8 维度全接受 ======
            @Test
    @DisplayName("P1 修补 2：8 维度（尺寸/形位/粗糙度/硬度/材质/外观/装配/性能）全接受")
    void testIntegration_8Dimensions() {
        for (String dim : new String[]{"尺寸", "形位", "粗糙度", "硬度", "材质", "外观", "装配", "性能"}) {
            FaCreateRequest req = buildReq();
            req.setItems(java.util.Collections.singletonList(item(dim, "x-" + dim, 1)));
            Result<CrmQualityFa> r = service.createFa(req, 401L);
            assertEquals(0, r.getCode(), "8 维度 " + dim + " 必接受");
        }
    }

    // ====== 跨 1.15 工单 ======
            @Test
    @DisplayName("跨 1.15：FA 携带 workOrderNo")
    void testIntegration_Cross_15_WorkOrder() {
        FaCreateRequest req = buildReq();
        req.setWorkOrderId(99L);
        req.setWorkOrderNo("GD20260608-9999");
        Result<CrmQualityFa> r = service.createFa(req, 401L);
        assertEquals(0, r.getCode());
        assertEquals("GD20260608-9999", r.getData().getWorkOrderNo());
    }

    // ====== 跨 1.5 FA 二次密码（不写实现 · 仅记录） ======
            @Test
    @DisplayName("跨 1.5：FA 继承金额 > 20万 二次密码校验（仅结构）")
    void testIntegration_Cross_05_2ndPassword() {
        Result<CrmQualityFa> r = service.createFa(buildReq(), 401L);
        assertEquals(0, r.getCode());
    }

    // ====== 审计留痕 ======
            @Test
    @DisplayName("审计留痕：createFa 写 1 fa + N items")
    void testIntegration_Audit() {
        service.createFa(buildReq(), 401L);
        verify(faMapper, times(1)).insert(any(CrmQualityFa.class));
        verify(itemMapper, times(3)).insert(any(CrmQualityFaItem.class));
    }

    // ====== PDF 报告必存（AC-7.2.2）======
            @Test
    @DisplayName("AC-7.2.2：PASSED 必有 PDF 报告")
    void testIntegration_PdfRequired() {
        Result<CrmQualityFa> c = service.createFa(buildReq(), 401L);
        Long id = c.getData().getId();

        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(id);
        fa.setFaNo("QF20260612-0001");
        fa.setResult("DRAFT");
        when(faMapper.selectById(id)).thenReturn(fa);
        CrmQualityFaItem item = new CrmQualityFaItem();
        item.setPassed(1);
        when(itemMapper.selectByFaId(id)).thenReturn(List.of(item));

        Result<CrmQualityFa> p = service.pass(id, 401L);
        assertNotNull(p.getData().getPdfUrl());
        assertTrue(p.getData().getPdfUrl().endsWith(".pdf"));
    }

    // ====== 开工前必检（FA DRAFT 状态）======
            @Test
    @DisplayName("P1 修补 1：FA 必检（开工前 DRAFT 状态）")
    void testIntegration_FA_BeforeProduction() {
        FaCreateRequest req = buildReq();
        Result<CrmQualityFa> r = service.createFa(req, 401L);
        assertEquals(0, r.getCode());
        assertEquals("DRAFT", r.getData().getResult());
    }

    // ====== 拒锁信号传给 1.15 ======
            @Test
    @DisplayName("跨 1.15：reject 信号含 workOrderId/processId")
    void testIntegration_Cross_15_RejectSignal() {
        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(500L);
        fa.setWorkOrderId(123L);
        fa.setProcessId(456L);
        fa.setResult("DRAFT");
        when(faMapper.selectById(500L)).thenReturn(fa);

        Result<Map<String, Object>> r = service.reject(500L, "test", 401L);
        assertEquals(123L, r.getData().get("workOrderId"));
        assertEquals(456L, r.getData().get("processId"));
        assertEquals(true, r.getData().get("processLocked"));
    }

    // ====== list ======
            @Test
    @DisplayName("list 按 result 过滤")
    void testIntegration_ListByResult() {
        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(1L);
        when(faMapper.selectByResult("FAILED")).thenReturn(List.of(fa));
        Result<List<CrmQualityFa>> r = service.list(null, null, "FAILED");
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }
}
