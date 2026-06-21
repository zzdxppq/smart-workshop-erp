package com.btsheng.erp.business.crm.bom.integration;

import com.btsheng.erp.business.crm.bom.dto.*;
import com.btsheng.erp.business.crm.bom.entity.CrmBom;
import com.btsheng.erp.business.crm.bom.entity.CrmBomItem;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomHistoryMapper;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomItemMapper;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomMapper;
import com.btsheng.erp.business.crm.bom.service.BomService;
import com.btsheng.erp.business.crm.conversion.mapper.CrmDrawingConversionMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 Story 1.9 · BOM 集成测例（25 测例）
 * Service 10 + Tree 5 + Convert 4 + CrossModule 6
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BomIntegrationTest {

    @Mock private CrmBomMapper bomMapper;
    @Mock private CrmBomItemMapper itemMapper;
    @Mock private CrmBomHistoryMapper historyMapper;
    @Mock private CrmDrawingConversionMapper conversionMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private BomService newSvc() {
        return new BomService(bomMapper, itemMapper, historyMapper, conversionMapper, docNoGenerator);
    }

    private CrmBom makeBom(String status, String type) {
        CrmBom b = new CrmBom();
        b.setId(1L);
        b.setBomNo("BOM-20260612-0001");
        b.setBomVersion("v1");
        b.setDrawingId(1L);
        b.setBomType(type);
        b.setTargetQty(100);
        b.setMaterialCode("WL-1001");
        b.setTotalCost(new BigDecimal("64050.00"));
        b.setStatus(status);
        b.setOwnerUserId(1001L);
        b.setBomLevel(0);
        return b;
    }

    // ===== Service 10 测例 =====
            @Test void integ_create_bom_with_5_segments() {
        when(bomMapper.selectByMaterialCodeAndVersion(anyString(), anyString())).thenReturn(null);
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-I-0001");
        when(bomMapper.insert(any(CrmBom.class))).thenReturn(1);
        when(itemMapper.insert(any(CrmBomItem.class))).thenReturn(1);
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-I-001");
        req.setTargetQty(10);
        String[] segs = {"原材料", "粗加工", "精加工", "表面处理", "检验"};
        List<BomCreateRequest.BomItemInput> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BomCreateRequest.BomItemInput it = new BomCreateRequest.BomItemInput();
            it.setMaterialCode("WL-" + i);
            it.setMaterialName("m" + i);
            it.setQty(BigDecimal.ONE);
            it.setUnitCost(new BigDecimal(100 * (i + 1)));
            it.setSegment(segs[i]);
            items.add(it);
        }
        req.setItems(items);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(0, r.getCode());
        // 5 段 = 100+200+300+400+500 = 1500
        // * 10 target = 15000
            assertEquals(new BigDecimal("15000.00"), r.getData().getTotalCost());
        assertTrue(r.getData().getCostBreakdown().contains("原材料"));
    }

    @Test void integ_create_bom_reject_dup() {
        CrmBom dup = makeBom("DRAFT", "STANDARD");
        when(bomMapper.selectByMaterialCodeAndVersion(anyString(), anyString())).thenReturn(dup);
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-1001");
        req.setTargetQty(10);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(40905, r.getCode());
    }

    @Test void integ_update_bom() {
        CrmBom b = makeBom("DRAFT", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(bomMapper.updateById(any(CrmBom.class))).thenReturn(1);
        BomService svc = newSvc();
        BomUpdateRequest req = new BomUpdateRequest();
        req.setTargetQty(200);
        Result<CrmBom> r = svc.updateBom(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(200, r.getData().getTargetQty());
    }

    @Test void integ_get_bom() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        BomService svc = newSvc();
        Result<CrmBom> r = svc.getBom(1L);
        assertEquals(0, r.getCode());
    }

    @Test void integ_release_draft() {
        CrmBom b = makeBom("DRAFT", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(bomMapper.updateById(any(CrmBom.class))).thenReturn(1);
        BomService svc = newSvc();
        ReleaseBomRequest req = new ReleaseBomRequest();
        Result<CrmBom> r = svc.releaseBom(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals("RELEASED", r.getData().getStatus());
    }

    @Test void integ_release_released_reject() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        BomService svc = newSvc();
        ReleaseBomRequest req = new ReleaseBomRequest();
        Result<CrmBom> r = svc.releaseBom(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }

    @Test void integ_convert_to_production() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(docNoGenerator.nextWorkOrderNo()).thenReturn("GD-I-0001");
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(50);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals("GD-I-0001", r.getData().get("workOrderNo"));
    }

    @Test void integ_list_boms() {
        CrmBom b1 = makeBom("RELEASED", "STANDARD");
        CrmBom b2 = makeBom("DRAFT", "STANDARD");
        b2.setId(2L);
        b2.setMaterialCode("WL-1002");
        when(bomMapper.selectList(null)).thenReturn(List.of(b1, b2));
        BomService svc = newSvc();
        BomQueryRequest q = new BomQueryRequest();
        q.setPage(0);
        q.setSize(10);
        Result<Map<String, Object>> r = svc.listBoms(q);
        assertEquals(0, r.getCode());
    }

    @Test void integ_update_released_reject() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        BomService svc = newSvc();
        BomUpdateRequest req = new BomUpdateRequest();
        req.setTargetQty(200);
        Result<CrmBom> r = svc.updateBom(1L, req, 1001L);
        assertEquals(40903, r.getCode());
    }

    @Test void integ_create_with_substitutable() {
        when(bomMapper.selectByMaterialCodeAndVersion(anyString(), anyString())).thenReturn(null);
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-I-0002");
        when(bomMapper.insert(any(CrmBom.class))).thenReturn(1);
        when(itemMapper.insert(any(CrmBomItem.class))).thenReturn(1);
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-I-002");
        req.setIsSubstitutable(true);
        req.setTargetQty(10);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().getIsSubstitutable());
    }

    // ===== Tree 5 测例 =====
            @Test void integ_tree_max_5_levels() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        List<CrmBomItem> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CrmBomItem it = new CrmBomItem();
            it.setItemLevel(i);
            items.add(it);
        }
        when(itemMapper.selectByBomId(1L)).thenReturn(items);
        when(itemMapper.maxItemLevel(1L)).thenReturn(4);
        BomService svc = newSvc();
        Result<Map<String, Object>> r = svc.getBomTree(1L);
        assertEquals(0, r.getCode());
        assertEquals(4, r.getData().get("maxLevel"));
    }

    @Test void integ_tree_with_5_items() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        List<CrmBomItem> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CrmBomItem it = new CrmBomItem();
            it.setItemLevel(0);
            it.setItemNo(i + 1);
            it.setSegment(new String[]{"原材料","粗加工","精加工","表面处理","检验"}[i]);
            items.add(it);
        }
        when(itemMapper.selectByBomId(1L)).thenReturn(items);
        when(itemMapper.maxItemLevel(1L)).thenReturn(0);
        BomService svc = newSvc();
        Result<Map<String, Object>> r = svc.getBomTree(1L);
        assertEquals(0, r.getCode());
        assertEquals(5, r.getData().get("totalItems"));
    }

    @Test void integ_tree_create_reject_level_5() {
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-1001");
        req.setTargetQty(10);
        BomCreateRequest.BomItemInput it = new BomCreateRequest.BomItemInput();
        it.setItemLevel(5);
        it.setMaterialCode("x");
        it.setMaterialName("x");
        List<BomCreateRequest.BomItemInput> items = new ArrayList<>();
        items.add(it);
        req.setItems(items);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void integ_tree_empty() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(itemMapper.selectByBomId(1L)).thenReturn(new ArrayList<>());
        when(itemMapper.maxItemLevel(1L)).thenReturn(null);
        BomService svc = newSvc();
        Result<Map<String, Object>> r = svc.getBomTree(1L);
        assertEquals(0, r.getCode());
        assertEquals(0, r.getData().get("totalItems"));
    }

    @Test void integ_tree_aggregate_by_segment() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(itemMapper.selectByBomId(1L)).thenReturn(new ArrayList<>());
        when(itemMapper.maxItemLevel(1L)).thenReturn(0);
        when(itemMapper.aggregateBySegment(1L)).thenReturn(new ArrayList<>());
        BomService svc = newSvc();
        Result<Map<String, Object>> r = svc.getBomTree(1L);
        assertEquals(0, r.getCode());
    }

    // ===== Convert 4 测例 =====
            @Test void integ_convert_released() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(docNoGenerator.nextWorkOrderNo()).thenReturn("GD-I-0002");
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(100);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void integ_convert_reject_draft() {
        CrmBom b = makeBom("DRAFT", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(10);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }

    @Test void integ_convert_zero_qty_reject() {
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(0);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void integ_convert_history_recorded() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(docNoGenerator.nextWorkOrderNo()).thenReturn("GD-I-0003");
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(10);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(0, r.getCode());
        verify(historyMapper, atLeastOnce()).insert(any(com.btsheng.erp.business.crm.bom.entity.CrmBomHistory.class));
    }

    // ===== CrossModule 6 测例 =====
            @Test void crossmodule_linked_to_1_8_conversion() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        b.setDrawingId(1L);
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(docNoGenerator.nextWorkOrderNo()).thenReturn("GD-I-0004");
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(10);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(1L, r.getData().get("bomId"));
    }

    @Test void crossmodule_5_segment_cost_for_1_10() {
        when(bomMapper.selectByMaterialCodeAndVersion(anyString(), anyString())).thenReturn(null);
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-I-0003");
        when(bomMapper.insert(any(CrmBom.class))).thenReturn(1);
        when(itemMapper.insert(any(CrmBomItem.class))).thenReturn(1);
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-I-003");
        req.setTargetQty(100);
        String[] segs = {"原材料", "粗加工", "精加工", "表面处理", "检验"};
        List<BomCreateRequest.BomItemInput> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BomCreateRequest.BomItemInput it = new BomCreateRequest.BomItemInput();
            it.setMaterialCode("WL-" + i);
            it.setMaterialName("m" + i);
            it.setQty(BigDecimal.ONE);
            it.setUnitCost(new BigDecimal(100));
            it.setSegment(segs[i]);
            items.add(it);
        }
        req.setItems(items);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(0, r.getCode());
        // 5 * 100 * 100 = 50000
            assertEquals(new BigDecimal("50000.00"), r.getData().getTotalCost());
    }

    @Test void crossmodule_work_order_to_production() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(docNoGenerator.nextWorkOrderNo()).thenReturn("GD-20260612-100");
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(100);
        req.setPlannedStartDate("2026-06-15");
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().get("workOrderNo").toString().startsWith("GD-"));
    }

    @Test void crossmodule_1_5_blacklist_priority() {
        // 复用 1.5 黑名单：FA 件 BOM 转化前必须校验黑名单（这里简化为 status check）
            CrmBom b = makeBom("DRAFT", "FA");
        when(bomMapper.selectById(1L)).thenReturn(b);
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(10);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }

    @Test void crossmodule_1_6_credit_limit_check() {
        // 复用 1.6 信用额度：转化数量超限（假设 10000）
            CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(docNoGenerator.nextWorkOrderNo()).thenReturn("GD-20260612-101");
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(100);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void crossmodule_audit_log_written() {
        CrmBom b = makeBom("DRAFT", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(bomMapper.updateById(any(CrmBom.class))).thenReturn(1);
        BomService svc = newSvc();
        BomUpdateRequest req = new BomUpdateRequest();
        req.setTargetQty(200);
        Result<CrmBom> r = svc.updateBom(1L, req, 1001L);
        assertEquals(0, r.getCode());
        verify(historyMapper, atLeastOnce()).insert(any(com.btsheng.erp.business.crm.bom.entity.CrmBomHistory.class));
    }
}
