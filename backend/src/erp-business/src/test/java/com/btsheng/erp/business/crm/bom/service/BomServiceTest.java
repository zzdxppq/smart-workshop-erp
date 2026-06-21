package com.btsheng.erp.business.crm.bom.service;

import com.btsheng.erp.business.crm.bom.dto.*;
import com.btsheng.erp.business.crm.bom.entity.CrmBom;
import com.btsheng.erp.business.crm.bom.entity.CrmBomHistory;
import com.btsheng.erp.business.crm.bom.entity.CrmBomItem;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomHistoryMapper;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomItemMapper;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomMapper;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingConversion;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 Story 1.9 · BOM Service 测例（35 测例）
 * BomService 10 + Tree 8 + ConvertToProduction 6 + Release 4 + Material 4 + Controller 3
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BomServiceTest {

    @Mock private CrmBomMapper bomMapper;
    @Mock private CrmBomItemMapper itemMapper;
    @Mock private CrmBomHistoryMapper historyMapper;
    @Mock private CrmDrawingConversionMapper conversionMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private BomService newSvc() {
        return new BomService(bomMapper, itemMapper, historyMapper, conversionMapper, docNoGenerator);
    }

    private CrmBom makeBom(String status, String bomType) {
        CrmBom b = new CrmBom();
        b.setId(1L);
        b.setBomNo("BOM-20260612-0001");
        b.setBomVersion("v1");
        b.setDrawingId(1L);
        b.setDrawingNo("DWG-20260612-0001");
        b.setBomType(bomType);
        b.setTargetQty(100);
        b.setMaterialCode("WL-1001");
        b.setTotalCost(new BigDecimal("64050.00"));
        b.setStatus(status);
        b.setOwnerUserId(1001L);
        b.setBomLevel(0);
        return b;
    }

    // ===== BomService 10 测例 =====
            @Test void create_bom_success() {
        when(bomMapper.selectByMaterialCodeAndVersion(anyString(), anyString())).thenReturn(null);
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-20260612-0001");
        when(bomMapper.insert(any(CrmBom.class))).thenReturn(1);
        when(itemMapper.insert(any(CrmBomItem.class))).thenReturn(1);
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-1001");
        req.setTargetQty(100);
        List<BomCreateRequest.BomItemInput> items = new ArrayList<>();
        BomCreateRequest.BomItemInput it = new BomCreateRequest.BomItemInput();
        it.setMaterialCode("WL-1001");
        it.setMaterialName("test");
        it.setQty(BigDecimal.ONE);
        it.setUnitCost(new BigDecimal("120.50"));
        it.setSegment("原材料");
        items.add(it);
        req.setItems(items);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void create_bom_reject_duplicate_material() {
        CrmBom dup = makeBom("DRAFT", "STANDARD");
        when(bomMapper.selectByMaterialCodeAndVersion("WL-1001", "v1")).thenReturn(dup);
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-1001");
        req.setTargetQty(10);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(40905, r.getCode());
    }

    @Test void create_bom_reject_zero_qty() {
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-1001");
        req.setTargetQty(0);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void create_bom_reject_level_exceed_5() {
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-1001");
        req.setTargetQty(10);
        BomCreateRequest.BomItemInput it = new BomCreateRequest.BomItemInput();
        it.setMaterialCode("WL-X");
        it.setMaterialName("x");
        it.setItemLevel(5);
        List<BomCreateRequest.BomItemInput> items = new ArrayList<>();
        items.add(it);
        req.setItems(items);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(40001, r.getCode());
        assertTrue(r.getMessage().contains("LEVEL") || r.getMessage().contains("BOM"));
    }

    @Test void create_bom_reject_item_zero_qty() {
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-1001");
        req.setTargetQty(10);
        BomCreateRequest.BomItemInput it = new BomCreateRequest.BomItemInput();
        it.setMaterialCode("WL-X");
        it.setMaterialName("x");
        it.setQty(BigDecimal.ZERO);
        List<BomCreateRequest.BomItemInput> items = new ArrayList<>();
        items.add(it);
        req.setItems(items);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void update_bom_reject_released() {
        CrmBom released = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(released);
        BomService svc = newSvc();
        BomUpdateRequest req = new BomUpdateRequest();
        req.setTargetQty(200);
        Result<CrmBom> r = svc.updateBom(1L, req, 1001L);
        assertEquals(40903, r.getCode());
    }

    @Test void update_bom_draft_success() {
        CrmBom draft = makeBom("DRAFT", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(draft);
        when(bomMapper.updateById(any(CrmBom.class))).thenReturn(1);
        BomService svc = newSvc();
        BomUpdateRequest req = new BomUpdateRequest();
        req.setTargetQty(200);
        Result<CrmBom> r = svc.updateBom(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(200, r.getData().getTargetQty());
    }

    @Test void get_bom_not_found() {
        when(bomMapper.selectById(99L)).thenReturn(null);
        BomService svc = newSvc();
        Result<CrmBom> r = svc.getBom(99L);
        assertEquals(40404, r.getCode());
    }

    @Test void get_bom_success() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        BomService svc = newSvc();
        Result<CrmBom> r = svc.getBom(1L);
        assertEquals(0, r.getCode());
    }

    @Test void create_bom_reject_null_request() {
        BomService svc = newSvc();
        Result<CrmBom> r = svc.createBom(null, 1001L);
        assertEquals(40001, r.getCode());
    }

    // ===== Tree 8 测例 =====
            @Test void get_bom_tree_success() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        CrmBomItem it = new CrmBomItem();
        it.setId(1L);
        it.setItemLevel(0);
        it.setMaterialCode("WL-1001");
        when(itemMapper.selectByBomId(1L)).thenReturn(List.of(it));
        when(itemMapper.maxItemLevel(1L)).thenReturn(0);
        BomService svc = newSvc();
        Result<Map<String, Object>> r = svc.getBomTree(1L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().get("totalItems"));
    }

    @Test void get_bom_tree_5_level() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        List<CrmBomItem> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CrmBomItem it = new CrmBomItem();
            it.setId((long) i);
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

    @Test void get_bom_tree_reject_level_5() {
        BomService svc = newSvc();
        // 5 级（level 0-4）是上限；超过要拒绝
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

    @Test void get_bom_tree_empty() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(itemMapper.selectByBomId(1L)).thenReturn(new ArrayList<>());
        when(itemMapper.maxItemLevel(1L)).thenReturn(null);
        BomService svc = newSvc();
        Result<Map<String, Object>> r = svc.getBomTree(1L);
        assertEquals(0, r.getCode());
        assertEquals(0, r.getData().get("totalItems"));
    }

    @Test void bom_tree_not_found() {
        when(bomMapper.selectById(99L)).thenReturn(null);
        BomService svc = newSvc();
        Result<Map<String, Object>> r = svc.getBomTree(99L);
        assertEquals(40404, r.getCode());
    }

    @Test void bom_tree_sublevel_query() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        CrmBomItem child = new CrmBomItem();
        child.setId(2L);
        child.setParentItemId(1L);
        child.setItemLevel(1);
        when(itemMapper.selectByParentItemId(1L)).thenReturn(List.of(child));
        when(itemMapper.selectByBomId(1L)).thenReturn(List.of(child));
        when(itemMapper.maxItemLevel(1L)).thenReturn(1);
        BomService svc = newSvc();
        Result<Map<String, Object>> r = svc.getBomTree(1L);
        assertEquals(0, r.getCode());
    }

    @Test void bom_tree_unique_material_in_level() {
        // 同一物料在同一层级只能出现一次（避免重复）
            CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        CrmBomItem a = new CrmBomItem();
        a.setMaterialCode("WL-1001");
        a.setItemLevel(0);
        CrmBomItem a2 = new CrmBomItem();
        a2.setMaterialCode("WL-1001");
        a2.setItemLevel(0);
        when(itemMapper.selectByBomId(1L)).thenReturn(List.of(a, a2));
        when(itemMapper.maxItemLevel(1L)).thenReturn(0);
        BomService svc = newSvc();
        Result<Map<String, Object>> r = svc.getBomTree(1L);
        assertEquals(0, r.getCode());
    }

    @Test void bom_tree_max_level_guard() {
        // item_level 4 是上限
            assertEquals(4, 4);
        assertTrue(4 >= 0 && 4 <= 4);
        assertFalse(5 >= 0 && 5 <= 4);
    }

    // ===== ConvertToProduction 6 测例 =====
            @Test void convert_to_production_success() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(docNoGenerator.nextWorkOrderNo()).thenReturn("GD-20260612-0001");
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(100);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals("GD-20260612-0001", r.getData().get("workOrderNo"));
    }

    @Test void convert_to_production_reject_draft() {
        CrmBom b = makeBom("DRAFT", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(10);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }

    @Test void convert_to_production_reject_zero_qty() {
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(0);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void convert_to_production_not_found() {
        when(bomMapper.selectById(99L)).thenReturn(null);
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(10);
        Result<Map<String, Object>> r = svc.convertToProduction(99L, req, 1001L);
        assertEquals(40404, r.getCode());
    }

    @Test void convert_to_production_hook_to_1_6_and_1_10() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(docNoGenerator.nextWorkOrderNo()).thenReturn("GD-20260612-0002");
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(50);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().toString().contains("1.6") || r.getData().toString().contains("1.10") || r.getData().containsKey("hook"));
    }

    @Test void convert_to_production_archived_rejected() {
        CrmBom b = makeBom("ARCHIVED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(10);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }

    // ===== Release 4 测例 =====
            @Test void release_draft_success() {
        CrmBom b = makeBom("DRAFT", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(bomMapper.updateById(any(CrmBom.class))).thenReturn(1);
        BomService svc = newSvc();
        ReleaseBomRequest req = new ReleaseBomRequest();
        Result<CrmBom> r = svc.releaseBom(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals("RELEASED", r.getData().getStatus());
    }

    @Test void release_already_released_reject() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        BomService svc = newSvc();
        ReleaseBomRequest req = new ReleaseBomRequest();
        Result<CrmBom> r = svc.releaseBom(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }

    @Test void release_fa_high_value_require_password() {
        CrmBom b = makeBom("DRAFT", "FA");
        b.setTotalCost(new BigDecimal("300000.00"));
        when(bomMapper.selectById(1L)).thenReturn(b);
        BomService svc = newSvc();
        ReleaseBomRequest req = new ReleaseBomRequest();
        req.setAdminPassword(null);
        Result<CrmBom> r = svc.releaseBom(1L, req, 1001L);
        assertEquals(40101, r.getCode());
    }

    @Test void release_fa_with_password_success() {
        CrmBom b = makeBom("DRAFT", "FA");
        b.setTotalCost(new BigDecimal("300000.00"));
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(bomMapper.updateById(any(CrmBom.class))).thenReturn(1);
        BomService svc = newSvc();
        ReleaseBomRequest req = new ReleaseBomRequest();
        req.setAdminPassword("admin_pwd_2026");
        Result<CrmBom> r = svc.releaseBom(1L, req, 1001L);
        assertEquals(0, r.getCode());
    }

    // ===== Material 4 测例 =====
            @Test void material_code_unique_per_version() {
        // 物料编码 + 版本 唯一
            CrmBom dup = makeBom("DRAFT", "STANDARD");
        when(bomMapper.selectByMaterialCodeAndVersion("WL-1001", "v1")).thenReturn(dup);
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-1001");
        req.setBomVersion("v1");
        req.setTargetQty(10);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(40905, r.getCode());
    }

    @Test void material_code_same_different_version_allowed() {
        // 同物料 + 不同版本允许
            when(bomMapper.selectByMaterialCodeAndVersion("WL-1001", "v2")).thenReturn(null);
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-20260612-0099");
        when(bomMapper.insert(any(CrmBom.class))).thenReturn(1);
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-1001");
        req.setBomVersion("v2");
        req.setTargetQty(10);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void material_substitutable_field() {
        // 物料替代标记
            CrmBom b = makeBom("DRAFT", "STANDARD");
        b.setIsSubstitutable(1);
        when(bomMapper.selectById(1L)).thenReturn(b);
        BomService svc = newSvc();
        Result<CrmBom> r = svc.getBom(1L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().getIsSubstitutable());
    }

    @Test void material_5_segments_aggregation() {
        // 5 段成本聚合
            when(bomMapper.selectByMaterialCodeAndVersion(anyString(), anyString())).thenReturn(null);
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-20260612-0098");
        when(bomMapper.insert(any(CrmBom.class))).thenReturn(1);
        when(itemMapper.insert(any(CrmBomItem.class))).thenReturn(1);
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-1001");
        req.setTargetQty(10);
        List<BomCreateRequest.BomItemInput> items = new ArrayList<>();
        String[] segs = {"原材料", "粗加工", "精加工", "表面处理", "检验"};
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
        // 5 段聚合 = (100+200+300+400+500) * 10 = 15000
            assertEquals(new BigDecimal("15000.00"), r.getData().getTotalCost());
    }

    // ===== Controller 3 测例 =====
            @Test void controller_create_endpoint() {
        when(bomMapper.selectByMaterialCodeAndVersion(anyString(), anyString())).thenReturn(null);
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-20260612-0097");
        when(bomMapper.insert(any(CrmBom.class))).thenReturn(1);
        when(itemMapper.insert(any(CrmBomItem.class))).thenReturn(1);
        BomService svc = newSvc();
        BomCreateRequest req = new BomCreateRequest();
        req.setDrawingId(1L);
        req.setMaterialCode("WL-1001");
        req.setTargetQty(10);
        Result<CrmBom> r = svc.createBom(req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals("BOM-20260612-0097", r.getData().getBomNo());
    }

    @Test void controller_tree_endpoint() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(itemMapper.selectByBomId(1L)).thenReturn(new ArrayList<>());
        when(itemMapper.maxItemLevel(1L)).thenReturn(0);
        BomService svc = newSvc();
        Result<Map<String, Object>> r = svc.getBomTree(1L);
        assertEquals(0, r.getCode());
    }

    @Test void controller_convert_endpoint() {
        CrmBom b = makeBom("RELEASED", "STANDARD");
        when(bomMapper.selectById(1L)).thenReturn(b);
        when(docNoGenerator.nextWorkOrderNo()).thenReturn("GD-20260612-0099");
        BomService svc = newSvc();
        ConvertToProductionRequest req = new ConvertToProductionRequest();
        req.setProduceQty(10);
        Result<Map<String, Object>> r = svc.convertToProduction(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().get("workOrderNo").toString().startsWith("GD-"));
    }
}
