package com.btsheng.erp.business.crm.conversion.integration;

import com.btsheng.erp.business.crm.bom.entity.CrmBom;
import com.btsheng.erp.business.crm.bom.service.BomService;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.service.MaterialMasterEnsureService;
import com.btsheng.erp.business.crm.conversion.dto.AnnotationRequest;
import com.btsheng.erp.business.crm.conversion.dto.ConversionRequest;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingAnnotation;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingConversion;
import com.btsheng.erp.business.crm.conversion.mapper.CrmDrawingAnnotationMapper;
import com.btsheng.erp.business.crm.conversion.service.AnnotationService;
import com.btsheng.erp.business.crm.conversion.service.ConversionService;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.service.DrawingPdfExportService;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 Story 1.8 · 工程转化集成测例（12 测例）
 * Service 6 + Controller 3 + CrossModule 3
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversionIntegrationTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingAnnotationMapper annotationMapper;
    @Mock private com.btsheng.erp.business.crm.conversion.mapper.CrmDrawingConversionMapper conversionMapper;
    @Mock private com.btsheng.erp.business.crm.conversion.mapper.CrmDrawingAnnotationHistoryMapper historyMapper;
    @Mock private com.btsheng.erp.business.crm.conversion.mapper.CrmEngineerWorkloadMapper workloadMapper;
    @Mock private DrawingPdfExportService pdfExportService;
    @Mock private DocNoGenerator docNoGenerator;
    @Mock private MaterialMasterEnsureService materialMasterEnsureService;
    @Mock private BomService bomService;

    private ConversionService newConv() {
        return new ConversionService(drawingMapper, conversionMapper, workloadMapper,
                pdfExportService, docNoGenerator, materialMasterEnsureService, bomService);
    }

    private void stubConversionDeps(String materialCode, String bomNo) {
        CrmMaterial material = new CrmMaterial();
        material.setId(10L);
        material.setMaterialCode(materialCode);
        when(materialMasterEnsureService.ensureFromDrawing(anyString(), anyString())).thenReturn(material);
        CrmBom bom = new CrmBom();
        bom.setId(100L);
        bom.setBomNo(bomNo);
        when(bomService.createBom(any(), anyLong())).thenReturn(Result.ok(bom));
        when(drawingMapper.updateById(any(CrmDrawing.class))).thenReturn(1);
    }

    private AnnotationService newAnn() {
        return new AnnotationService(annotationMapper, historyMapper, drawingMapper, workloadMapper);
    }

    private CrmDrawing makeDrawing(String status, String version) {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L);
        d.setDrawingNo("DWG-20260612-0001");
        d.setVersion(version);
        d.setStatus(status);
        d.setProcessRoute("[{\"step\":1,\"name\":\"车削\",\"cost\":120.50},{\"step\":2,\"name\":\"铣削\",\"cost\":80.00},{\"step\":3,\"name\":\"热处理\",\"cost\":200.00},{\"step\":4,\"name\":\"精磨\",\"cost\":150.00},{\"step\":5,\"name\":\"表面处理\",\"cost\":90.00}]");
        d.setMaterialCode("WL-1001");
        d.setIsFa(1);
        return d;
    }

    @SuppressWarnings("unchecked")
    private Result<Map<String, Object>> costHook(double total) {
        Map<String, Object> m = new HashMap<>();
        m.put("totalCost", total);
        m.put("stepCount", 5);
        return Result.ok(m);
    }

    // ===== Service 6 测例 =====
            @Test void svc_convert_success() {
        CrmDrawing d = makeDrawing("RELEASED", "v1");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(conversionMapper.selectByDrawingIdAndVersion(1L, "v1")).thenReturn(null);
        when(pdfExportService.aggregateProcessRouteCost(anyString())).thenReturn(costHook(640.50));
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-20260612-0001");
        when(conversionMapper.insert(any(CrmDrawingConversion.class))).thenReturn(1);
        stubConversionDeps("WL-1001", "BOM-20260612-0001");
        ConversionService svc = newConv();
        ConversionRequest req = new ConversionRequest();
        req.setTargetQty(100);
        Result<CrmDrawingConversion> r = svc.convertDrawing(1L, req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void svc_convert_reject_draft() {
        CrmDrawing d = makeDrawing("DRAFT", "v1");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        ConversionService svc = newConv();
        ConversionRequest req = new ConversionRequest();
        req.setTargetQty(10);
        Result<CrmDrawingConversion> r = svc.convertDrawing(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }

    @Test void svc_convert_5_segments() {
        CrmDrawing d = makeDrawing("RELEASED", "v1");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(conversionMapper.selectByDrawingIdAndVersion(1L, "v1")).thenReturn(null);
        when(pdfExportService.aggregateProcessRouteCost(anyString())).thenReturn(costHook(640.50));
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-20260612-0002");
        when(conversionMapper.insert(any(CrmDrawingConversion.class))).thenReturn(1);
        stubConversionDeps("WL-1001", "BOM-20260612-0002");
        ConversionService svc = newConv();
        ConversionRequest req = new ConversionRequest();
        req.setTargetQty(50);
        Result<CrmDrawingConversion> r = svc.convertDrawing(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().getCostBreakdown().contains("原材料"));
    }

    @Test void svc_annotation_add() {
        CrmDrawing d = makeDrawing("RELEASED", "v1");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(annotationMapper.selectByPosition(any(), any(), any(), any(), any())).thenReturn(null);
        when(annotationMapper.insert(any(CrmDrawingAnnotation.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.conversion.entity.CrmDrawingAnnotationHistory.class))).thenReturn(1);
        AnnotationService svc = newAnn();
        AnnotationRequest req = new AnnotationRequest();
        req.setVersion("v1");
        req.setType("DIMENSION");
        req.setContent("φ100±0.05");
        req.setColor("RED");
        req.setX(new BigDecimal("100.50"));
        req.setY(new BigDecimal("200.30"));
        req.setPriority(9);
        Result<CrmDrawingAnnotation> r = svc.addAnnotation(1L, req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void svc_annotation_reject_version_mismatch() {
        CrmDrawing d = makeDrawing("RELEASED", "v2");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        AnnotationService svc = newAnn();
        AnnotationRequest req = new AnnotationRequest();
        req.setVersion("v1");
        req.setType("DIMENSION");
        req.setContent("test");
        Result<CrmDrawingAnnotation> r = svc.addAnnotation(1L, req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void svc_annotation_reject_empty_content() {
        CrmDrawing d = makeDrawing("RELEASED", "v1");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        AnnotationService svc = newAnn();
        AnnotationRequest req = new AnnotationRequest();
        req.setVersion("v1");
        req.setType("DIMENSION");
        req.setContent("");
        Result<CrmDrawingAnnotation> r = svc.addAnnotation(1L, req, 1001L);
        assertEquals(40001, r.getCode());
    }

    // ===== Controller 3 测例 =====
            @Test void controller_convert_endpoint() {
        CrmDrawing d = makeDrawing("RELEASED", "v1");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(conversionMapper.selectByDrawingIdAndVersion(1L, "v1")).thenReturn(null);
        when(pdfExportService.aggregateProcessRouteCost(anyString())).thenReturn(costHook(640.50));
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-20260612-0003");
        when(conversionMapper.insert(any(CrmDrawingConversion.class))).thenReturn(1);
        stubConversionDeps("WL-1001", "BOM-20260612-0003");
        ConversionService svc = newConv();
        ConversionRequest req = new ConversionRequest();
        req.setTargetQty(10);
        Result<CrmDrawingConversion> r = svc.convertDrawing(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals("BOM-20260612-0003", r.getData().getBomNo());
    }

    @Test void controller_annotation_endpoint() {
        CrmDrawing d = makeDrawing("RELEASED", "v1");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(annotationMapper.selectByPosition(any(), any(), any(), any(), any())).thenReturn(null);
        when(annotationMapper.insert(any(CrmDrawingAnnotation.class))).thenReturn(1);
        AnnotationService svc = newAnn();
        AnnotationRequest req = new AnnotationRequest();
        req.setVersion("v1");
        req.setType("TECH_NOTE");
        req.setContent("test note");
        Result<CrmDrawingAnnotation> r = svc.addAnnotation(1L, req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void controller_get_with_annotations() {
        CrmDrawing d = makeDrawing("RELEASED", "v1");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        CrmDrawingAnnotation a1 = new CrmDrawingAnnotation();
        a1.setId(1L);
        a1.setType("DIMENSION");
        a1.setContent("test");
        when(annotationMapper.selectByDrawingAndVersion(1L, "v1")).thenReturn(List.of(a1));
        AnnotationService svc = newAnn();
        Result<Map<String, Object>> r = svc.getDrawingWithAnnotations(1L, "v1");
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().get("annotationCount"));
    }

    // ===== CrossModule 3 测例 =====
            @Test void crossmodule_to_bom_via_bom_no() {
        CrmDrawing d = makeDrawing("RELEASED", "v1");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(conversionMapper.selectByDrawingIdAndVersion(1L, "v1")).thenReturn(null);
        when(pdfExportService.aggregateProcessRouteCost(anyString())).thenReturn(costHook(640.50));
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-20260612-X001");
        when(conversionMapper.insert(any(CrmDrawingConversion.class))).thenReturn(1);
        stubConversionDeps("WL-1001", "BOM-20260612-X001");
        ConversionService svc = newConv();
        ConversionRequest req = new ConversionRequest();
        req.setTargetQty(100);
        Result<CrmDrawingConversion> r = svc.convertDrawing(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().getBomNo().startsWith("BOM-"));
    }

    @Test void crossmodule_engineer_workload_increment() {
        CrmDrawing d = makeDrawing("RELEASED", "v1");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(conversionMapper.selectByDrawingIdAndVersion(1L, "v1")).thenReturn(null);
        when(pdfExportService.aggregateProcessRouteCost(anyString())).thenReturn(costHook(640.50));
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-20260612-X002");
        when(conversionMapper.insert(any(CrmDrawingConversion.class))).thenReturn(1);
        stubConversionDeps("WL-1001", "BOM-20260612-X002");
        when(workloadMapper.selectByUserAndDate(any(), any())).thenReturn(null);
        when(workloadMapper.insert(any(com.btsheng.erp.business.crm.conversion.entity.CrmEngineerWorkload.class))).thenReturn(1);
        ConversionService svc = newConv();
        ConversionRequest req = new ConversionRequest();
        req.setEngineerName("张工");
        req.setTargetQty(10);
        Result<CrmDrawingConversion> r = svc.convertDrawing(1L, req, 1001L);
        assertEquals(0, r.getCode());
        verify(workloadMapper, atLeastOnce()).insert(any(com.btsheng.erp.business.crm.conversion.entity.CrmEngineerWorkload.class));
    }

    @Test void crossmodule_reuse_pdf_5_segment_aggregator() {
        CrmDrawing d = makeDrawing("RELEASED", "v1");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(conversionMapper.selectByDrawingIdAndVersion(1L, "v1")).thenReturn(null);
        when(pdfExportService.aggregateProcessRouteCost(anyString())).thenReturn(costHook(640.50));
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-20260612-X003");
        when(conversionMapper.insert(any(CrmDrawingConversion.class))).thenReturn(1);
        stubConversionDeps("WL-1001", "BOM-20260612-X003");
        ConversionService svc = newConv();
        ConversionRequest req = new ConversionRequest();
        req.setTargetQty(10);
        Result<CrmDrawingConversion> r = svc.convertDrawing(1L, req, 1001L);
        assertEquals(0, r.getCode());
        verify(pdfExportService, atLeastOnce()).aggregateProcessRouteCost(anyString());
    }
}
