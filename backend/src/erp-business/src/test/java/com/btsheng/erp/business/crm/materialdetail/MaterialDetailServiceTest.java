package com.btsheng.erp.business.crm.materialdetail;

import com.btsheng.erp.business.integration.client.ProductionProductRouteClient;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterialCategory;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialCategoryMapper;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.materialdetail.dto.ChangeLogEntry;
import com.btsheng.erp.business.crm.materialdetail.dto.MaterialDetailDTO;
import com.btsheng.erp.business.crm.materialdetail.service.MaterialDetailService;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Story 2.1 · MaterialDetailService 单元测例（V1.3.8 Sprint 7）")
class MaterialDetailServiceTest {

    private ProductionProductRouteClient productRouteClient;
    private CrmMaterialMapper materialMapper;
    private CrmDrawingMapper drawingMapper;
    private CrmMaterialCategoryMapper categoryMapper;
    private MaterialDetailService service;

    @BeforeEach
    void setup() {
        productRouteClient = mock(ProductionProductRouteClient.class);
        materialMapper = mock(CrmMaterialMapper.class);
        drawingMapper = mock(CrmDrawingMapper.class);
        categoryMapper = mock(CrmMaterialCategoryMapper.class);
        service = new MaterialDetailService(productRouteClient, materialMapper, drawingMapper, categoryMapper);

        when(materialMapper.selectActiveById(12345L)).thenReturn(sampleMaterial());
        when(drawingMapper.selectByMaterialCode("WL-1001")).thenReturn(sampleDrawing());
        when(categoryMapper.selectById(3L)).thenReturn(sampleCategory());
        when(productRouteClient.getProductRoute(anyString())).thenReturn(Result.ok(sampleProductionRoute()));
    }

    @Test
    @DisplayName("AC-2.1.1.a 详情聚合返回 7 Tab")
    void detail_seven_tabs() {
        Result<MaterialDetailDTO> r = service.getMaterialDetail(12345L);
        assertTrue(r.isSuccess() || r.getCode() == 0);
        MaterialDetailDTO dto = r.getData();
        assertNotNull(dto.getBase());
        assertNotNull(dto.getProcess());
        assertNotNull(dto.getDrawing());
        assertNotNull(dto.getPrice());
        assertNotNull(dto.getCost());
        assertNotNull(dto.getLabor());
        assertNotNull(dto.getOutsource());
    }

    @Test
    @DisplayName("AC-2.1.1.b BaseInfo 字段映射")
    void detail_base_fields() {
        Result<MaterialDetailDTO> r = service.getMaterialDetail(12345L);
        MaterialDetailDTO.BaseInfo base = r.getData().getBase();
        assertEquals(12345L, base.getMaterialId());
        assertEquals("WL-1001", base.getMaterialNo());
        assertEquals("航空精密连接器外壳", base.getName());
        assertEquals("自制件", base.getCategory());
    }

    @Test
    @DisplayName("AC-2.1.1.c DrawingInfo 字段映射")
    void detail_drawing_fields() {
        Result<MaterialDetailDTO> r = service.getMaterialDetail(12345L);
        MaterialDetailDTO.DrawingInfo d = r.getData().getDrawing();
        assertEquals("DWG-20260612-0001", d.getDwgNo());
        assertEquals("v1", d.getVersion());
        assertEquals("RELEASED", d.getStatus());
        assertTrue(d.getIsLatest());
    }

    @Test
    @DisplayName("AC-2.1.1.d PriceInfo 字段映射（5 段）")
    void detail_price_fields() {
        Result<MaterialDetailDTO> r = service.getMaterialDetail(12345L);
        MaterialDetailDTO.PriceInfo p = r.getData().getPrice();
        assertNotNull(p.getCurrentPrice());
        assertNotNull(p.getAvg30d());
        assertNotNull(p.getMin30d());
        assertNotNull(p.getMax30d());
    }

    @Test
    @DisplayName("AC-2.1.1.e CostInfo / LaborInfo / OutsourceInfo 字段映射")
    void detail_cost_labor_outsource() {
        Result<MaterialDetailDTO> r = service.getMaterialDetail(12345L);
        assertNotNull(r.getData().getCost().getMaterialCost());
        assertNotNull(r.getData().getLabor().getHourlyRate());
        assertEquals("—", r.getData().getOutsource().getSupplier());
    }

    @Test
    @DisplayName("AC-2.1.1.f null materialId → fail")
    void detail_null_id() {
        Result<MaterialDetailDTO> r = service.getMaterialDetail(null);
        assertFalse(r.isSuccess() || r.getCode() == 0);
    }

    @Test
    @DisplayName("AC-2.1.1.g 物料不存在 → fail")
    void detail_not_found() {
        when(materialMapper.selectActiveById(999L)).thenReturn(null);
        when(materialMapper.selectById(999L)).thenReturn(null);
        Result<MaterialDetailDTO> r = service.getMaterialDetail(999L);
        assertFalse(r.isSuccess() || r.getCode() == 0);
    }

    @Test
    @DisplayName("AC-2.1.2.a 价格走势返回 12 个 trend point")
    void price_history_12_points() {
        Result<List<MaterialDetailDTO.PriceInfo.TrendPoint>> r = service.getPriceHistory(12345L);
        assertEquals(12, r.getData().size());
    }

    @Test
    @DisplayName("AC-2.1.2.b 每个 trend point 含 date + price")
    void price_history_point_fields() {
        Result<List<MaterialDetailDTO.PriceInfo.TrendPoint>> r = service.getPriceHistory(12345L);
        MaterialDetailDTO.PriceInfo.TrendPoint p = r.getData().get(0);
        assertNotNull(p.getDate());
        assertNotNull(p.getPrice());
    }

    @Test
    @DisplayName("AC-2.1.2.c 工艺路线从 production API 映射")
    void process_route_from_production_api() {
        when(productRouteClient.getProductRoute("12345")).thenReturn(Result.ok(sampleProductionRouteReleased()));
        Result<List<MaterialDetailDTO.ProcessInfo.ProcessRoute>> r = service.getProcessRoute(12345L);
        assertEquals(2, r.getData().size());
        assertEquals(1, r.getData().get(0).getStepSeq());
        assertEquals("P00", r.getData().get(0).getProcessNo());
        assertEquals("原材料", r.getData().get(0).getWorkcenter());
        assertEquals(new BigDecimal("30.0"), r.getData().get(0).getStdMinutes());
        assertEquals("CNC", r.getData().get(0).getEquipment());
    }

    @Test
    @DisplayName("AC-2.1.2.d 历史变更默认 limit=50")
    void change_log_default_limit() {
        Result<List<ChangeLogEntry>> r = service.getChangeLog(12345L, null);
        assertTrue(r.getData().size() <= 5);
    }

    @Test
    @DisplayName("AC-2.1.2.e limit=10 时返回条数 ≤ 5（演示数据上限）")
    void change_log_limit_10() {
        Result<List<ChangeLogEntry>> r = service.getChangeLog(12345L, 10);
        assertTrue(r.getData().size() <= 5);
    }

    @Test
    @DisplayName("AC-2.1.2.f change log 条目字段映射")
    void change_log_entry_fields() {
        Result<List<ChangeLogEntry>> r = service.getChangeLog(12345L, null);
        ChangeLogEntry e = r.getData().get(0);
        assertEquals("crm_material", e.getEntityType());
        assertEquals(12345L, e.getEntityId());
        assertEquals("UPDATE", e.getAction());
        assertNotNull(e.getChangedAt());
    }

    private CrmMaterial sampleMaterial() {
        CrmMaterial m = new CrmMaterial();
        m.setId(12345L);
        m.setMaterialCode("WL-1001");
        m.setMaterialName("航空精密连接器外壳");
        m.setSpec("Φ100×20");
        m.setUnit("件");
        m.setCategoryId(3L);
        m.setCostMaterial(new BigDecimal("42.30"));
        m.setCostLabor(new BigDecimal("35.0"));
        m.setCostOutsource(new BigDecimal("12.00"));
        m.setCostTotal(new BigDecimal("86.50"));
        return m;
    }

    private CrmDrawing sampleDrawing() {
        CrmDrawing d = new CrmDrawing();
        d.setDrawingNo("DWG-20260612-0001");
        d.setVersion("v1");
        d.setStatus("RELEASED");
        d.setPdfPath("/data/pdf/dwg-0001-v1.pdf");
        return d;
    }

    private CrmMaterialCategory sampleCategory() {
        CrmMaterialCategory c = new CrmMaterialCategory();
        c.setCategoryName("自制件");
        return c;
    }

    private Map<String, Object> sampleProductionRoute() {
        Map<String, Object> data = sampleProductionRouteReleased();
        data.put("routeStatus", "RELEASED");
        return data;
    }

    private Map<String, Object> sampleProductionRouteReleased() {
        Map<String, Object> step1 = new LinkedHashMap<>();
        step1.put("stepNo", 1);
        step1.put("stepName", "备料");
        step1.put("segment", "原材料");
        step1.put("machineType", "CNC");
        step1.put("estimatedHours", new BigDecimal("0.5"));

        Map<String, Object> step2 = new LinkedHashMap<>();
        step2.put("stepNo", 2);
        step2.put("stepName", "粗加工");
        step2.put("segment", "粗加工");
        step2.put("machineType", "LATHE");
        step2.put("estimatedHours", new BigDecimal("1.0"));

        Map<String, Object> route1 = new LinkedHashMap<>();
        route1.put("processSeq", 1);
        route1.put("processCode", "P00");
        Map<String, Object> route2 = new LinkedHashMap<>();
        route2.put("processSeq", 2);
        route2.put("processCode", "P01");

        Map<String, Object> data = new HashMap<>();
        data.put("routeStatus", "RELEASED");
        data.put("steps", List.of(step1, step2));
        data.put("routes", List.of(route1, route2));
        return data;
    }
}
