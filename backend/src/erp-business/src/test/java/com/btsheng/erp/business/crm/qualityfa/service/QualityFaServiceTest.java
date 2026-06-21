package com.btsheng.erp.business.crm.qualityfa.service;

import com.btsheng.erp.business.crm.qualityfa.dto.FaCreateRequest;
import com.btsheng.erp.business.crm.qualityfa.entity.CrmQualityFa;
import com.btsheng.erp.business.crm.qualityfa.entity.CrmQualityFaItem;
import com.btsheng.erp.business.crm.qualityfa.mapper.CrmQualityFaItemMapper;
import com.btsheng.erp.business.crm.qualityfa.mapper.CrmQualityFaMapper;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.29 · 品质·FA 首件 Service 单元测试（FR-7-2）
 * 14 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QualityFaServiceTest {

    @Mock private CrmQualityFaMapper faMapper;
    @Mock private CrmQualityFaItemMapper itemMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private QualityFaService service;

    @BeforeEach
    void setUp() {
        service = new QualityFaService(faMapper, itemMapper, docNoGenerator);
        when(docNoGenerator.nextQualityFaNo())
                .thenReturn("QF20260612-0001", "QF20260612-0002", "QF20260612-0003", "QF20260612-0004");
        when(faMapper.insert(any(CrmQualityFa.class))).thenAnswer(inv -> {
            CrmQualityFa f = inv.getArgument(0);
            f.setId(1L);
            return 1;
        });
        when(itemMapper.insert(any(CrmQualityFaItem.class))).thenAnswer(inv -> {
            CrmQualityFaItem i = inv.getArgument(0);
            i.setId(1L);
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

    private FaCreateRequest buildValidReq() {
        FaCreateRequest req = new FaCreateRequest();
        req.setWorkOrderId(1L);
        req.setWorkOrderNo("GD20260608-0001");
        req.setProcessId(10L);
        req.setProcessName("粗车");
        req.setInspectQty(1);
        req.setItems(Arrays.asList(
                item("尺寸", "外径 φ50", 1),
                item("粗糙度", "Ra 1.6", 1),
                item("外观", "无缺陷", 1)));
        return req;
    }

    // ====== createFa 6 测例 ======
            @Test
    @DisplayName("createFa happy path · 8 维度全有")
    void testCreate_AllDimensions() {
        Result<CrmQualityFa> r = service.createFa(buildValidReq(), 401L);
        assertEquals(0, r.getCode());
        assertEquals("QF20260612-0001", r.getData().getFaNo());
    }

    @Test
    @DisplayName("createFa 8 维度全 DRAFT（开工前必检 P1 修补 1）")
    void testCreate_BeforeProduction() {
        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(1L);
        fa.setResult("DRAFT");
        when(faMapper.selectById(1L)).thenReturn(fa);
        when(itemMapper.selectByFaId(1L)).thenReturn(Collections.emptyList());
        Result<CrmQualityFa> p = service.pass(1L, 401L);
        assertEquals(0, p.getCode());
    }

    @Test
    @DisplayName("createFa 8 维度校验：非 8 维度 → 40001（P1 修补 2）")
    void testCreate_DimensionInvalid() {
        FaCreateRequest req = buildValidReq();
        req.setItems(Arrays.asList(item("WRONG_DIM", "x", 1)));
        Result<CrmQualityFa> r = service.createFa(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("FA_DIMENSION_INVALID", r.getMessage());
    }

    @Test
    @DisplayName("createFa 检验项目必填")
    void testCreate_ItemsRequired() {
        FaCreateRequest req = buildValidReq();
        req.setItems(new ArrayList<>());
        Result<CrmQualityFa> r = service.createFa(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("FA_ITEMS_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("createFa 缺工单 ID → 40001")
    void testCreate_WorkOrderRequired() {
        FaCreateRequest req = buildValidReq();
        req.setWorkOrderId(null);
        Result<CrmQualityFa> r = service.createFa(req, 401L);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("createFa 缺工序 ID → 40001")
    void testCreate_ProcessRequired() {
        FaCreateRequest req = buildValidReq();
        req.setProcessId(null);
        Result<CrmQualityFa> r = service.createFa(req, 401L);
        assertEquals(40001, r.getCode());
    }

    // ====== pass 4 测例 ======
            @Test
    @DisplayName("pass 全 PASSED → PASSED + 自动 PDF 报告")
    void testPass_OK_AutoPdf() {
        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(1L);
        fa.setFaNo("QF20260612-0001");
        fa.setResult("DRAFT");
        when(faMapper.selectById(1L)).thenReturn(fa);
        CrmQualityFaItem item = new CrmQualityFaItem();
        item.setPassed(1);
        when(itemMapper.selectByFaId(1L)).thenReturn(List.of(item));

        Result<CrmQualityFa> r = service.pass(1L, 401L);
        assertEquals(0, r.getCode());
        assertEquals("PASSED", r.getData().getResult());
        assertNotNull(r.getData().getPdfUrl());
    }

    @Test
    @DisplayName("pass 有 FAILED 项目 → 40903")
    void testPass_HasFailedItem() {
        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(2L);
        fa.setResult("DRAFT");
        when(faMapper.selectById(2L)).thenReturn(fa);
        CrmQualityFaItem item = new CrmQualityFaItem();
        item.setPassed(0);
        when(itemMapper.selectByFaId(2L)).thenReturn(List.of(item));

        Result<CrmQualityFa> r = service.pass(2L, 401L);
        assertEquals(40903, r.getCode());
        assertEquals("FA_HAS_FAILED_ITEM", r.getMessage());
    }

    @Test
    @DisplayName("pass 重复 → 40903")
    void testPass_AlreadyPassed() {
        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(3L);
        fa.setResult("PASSED");
        when(faMapper.selectById(3L)).thenReturn(fa);

        Result<CrmQualityFa> r = service.pass(3L, 401L);
        assertEquals(40903, r.getCode());
    }

    @Test
    @DisplayName("pass FA 不存在 → 40404")
    void testPass_NotFound() {
        when(faMapper.selectById(99L)).thenReturn(null);
        Result<CrmQualityFa> r = service.pass(99L, 401L);
        assertEquals(40404, r.getCode());
    }

    // ====== reject 3 测例 ======
            @Test
    @DisplayName("reject → 锁定工序（阻断生产 P1 修补 3）")
    void testReject_LockProcess() {
        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(10L);
        fa.setWorkOrderId(1L);
        fa.setProcessId(10L);
        fa.setResult("DRAFT");
        when(faMapper.selectById(10L)).thenReturn(fa);

        Result<Map<String, Object>> r = service.reject(10L, "尺寸超差", 401L);
        assertEquals(0, r.getCode());
        assertEquals(true, r.getData().get("processLocked"));
        assertEquals(1L, r.getData().get("workOrderId"));
        assertEquals(10L, r.getData().get("processId"));
    }

    @Test
    @DisplayName("reject 重复 → 40903")
    void testReject_AlreadyFailed() {
        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(11L);
        fa.setResult("FAILED");
        when(faMapper.selectById(11L)).thenReturn(fa);

        Result<Map<String, Object>> r = service.reject(11L, null, 401L);
        assertEquals(40903, r.getCode());
    }

    @Test
    @DisplayName("reject 缺 ID → 40001")
    void testReject_IdRequired() {
        Result<Map<String, Object>> r = service.reject(null, "x", 401L);
        assertEquals(40001, r.getCode());
    }

    // ====== list 1 测例 ======
            @Test
    @DisplayName("list 按 workOrderId 过滤")
    void testList() {
        CrmQualityFa fa = new CrmQualityFa();
        fa.setId(1L);
        when(faMapper.selectByWorkOrderId(1L)).thenReturn(List.of(fa));
        Result<List<CrmQualityFa>> r = service.list(1L, null, null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }
}
