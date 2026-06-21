package com.btsheng.erp.business.crm.conversion.integration;

import com.btsheng.erp.business.crm.bom.dto.BomSaveTreeRequest;
import com.btsheng.erp.business.crm.bom.entity.CrmBom;
import com.btsheng.erp.business.crm.bom.entity.CrmBomItem;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomItemMapper;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomMapper;
import com.btsheng.erp.business.crm.bom.service.BomService;
import com.btsheng.erp.business.crm.conversion.dto.ConversionRequest;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingConversion;
import com.btsheng.erp.business.crm.conversion.mapper.CrmDrawingConversionMapper;
import com.btsheng.erp.business.crm.conversion.mapper.CrmEngineerWorkloadMapper;
import com.btsheng.erp.business.crm.conversion.service.ConversionService;
import com.btsheng.erp.business.crm.drawing.dto.AttachmentDownloadPayload;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingAttachment;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingAttachmentMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.service.DrawingAttachmentService;
import com.btsheng.erp.business.crm.drawing.service.DrawingMinioFileService;
import com.btsheng.erp.business.crm.drawing.service.DrawingPdfExportService;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialCategoryMapper;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.materialbarcode.service.MaterialMasterEnsureService;
import com.btsheng.erp.business.crm.materialdetail.service.MaterialDetailService;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 工程师核心链路 E2E（Mock 集成）：
 * 上传 CAD 附件 → 工程转化(WL+BOM) → BOM 子件保存 → 工艺路线 RELEASED 过滤
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EngineeringWorkflowE2ETest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingConversionMapper conversionMapper;
    @Mock private CrmEngineerWorkloadMapper workloadMapper;
    @Mock private DrawingPdfExportService pdfExportService;
    @Mock private DocNoGenerator docNoGenerator;
    @Mock private MaterialMasterEnsureService materialMasterEnsureService;
    @Mock private BomService bomService;
    @Mock private CrmBomMapper bomMapper;
    @Mock private CrmBomItemMapper bomItemMapper;
    @Mock private CrmDrawingAttachmentMapper attachmentMapper;
    @Mock private DrawingMinioFileService fileStorage;
    @Mock private com.btsheng.erp.business.integration.client.ProductionProductRouteClient productRouteClient;
    @Mock private CrmMaterialMapper materialMapper;
    @Mock private CrmMaterialCategoryMapper categoryMapper;

    private ConversionService conversionService;
    private BomService bomServiceReal;
    private DrawingAttachmentService attachmentService;
    private MaterialDetailService materialDetailService;

    @BeforeEach
    void setUp() {
        conversionService = new ConversionService(drawingMapper, conversionMapper, workloadMapper,
                pdfExportService, docNoGenerator, materialMasterEnsureService, bomService);
        bomServiceReal = new BomService(bomMapper, bomItemMapper, mock(com.btsheng.erp.business.crm.bom.mapper.CrmBomHistoryMapper.class),
                conversionMapper, docNoGenerator);
        attachmentService = new DrawingAttachmentService(drawingMapper, attachmentMapper, fileStorage);
        materialDetailService = new MaterialDetailService(productRouteClient, materialMapper, drawingMapper, categoryMapper);
    }

    @Test
    void e2e_drawing_cad_convert_bom_save_route_filter() throws Exception {
        // === Step 0: RELEASED 图纸 ===
        CrmDrawing drawing = new CrmDrawing();
        drawing.setId(10L);
        drawing.setDrawingNo("DWG-20260620-E2E");
        drawing.setVersion("v1");
        drawing.setTitle("法兰盘 E2E");
        drawing.setStatus("RELEASED");
        drawing.setProcessRoute("[{\"name\":\"车削\",\"cost\":100}]");
        when(drawingMapper.selectById(10L)).thenReturn(drawing);

        // === Step 0b: CAD/CAM 附件 ===
        when(fileStorage.toMinioUri(anyString())).thenReturn("local:///tmp/cad/e2e.dxf");
        when(attachmentMapper.insert(any(CrmDrawingAttachment.class))).thenAnswer(inv -> {
            CrmDrawingAttachment a = inv.getArgument(0);
            a.setId(501L);
            return 1;
        });
        MockMultipartFile cad = new MockMultipartFile("file", "e2e-fixture.dxf", "application/dxf", "DXF-DATA".getBytes());
        Result<CrmDrawingAttachment> attachResult = attachmentService.uploadAttachment(10L, cad, 1001L);
        assertEquals(0, attachResult.getCode());
        assertEquals("DXF", attachResult.getData().getFileType());
        when(attachmentMapper.selectById(501L)).thenReturn(attachResult.getData());
        when(fileStorage.readBytes(anyString())).thenReturn("DXF-DATA".getBytes());
        Result<AttachmentDownloadPayload> dl = attachmentService.downloadAttachment(501L);
        assertEquals(0, dl.getCode());
        assertArrayEquals("DXF-DATA".getBytes(), dl.getData().getData());

        // === Step 1: 工程转化 ===
        when(conversionMapper.selectByDrawingIdAndVersion(10L, "v1")).thenReturn(null);
        Map<String, Object> cost = new HashMap<>();
        cost.put("totalCost", 640.0);
        when(pdfExportService.aggregateProcessRouteCost(anyString())).thenReturn(Result.ok(cost));
        when(docNoGenerator.nextBomNo()).thenReturn("BOM-20260620-E2E1");
        when(docNoGenerator.nextMaterialCode()).thenReturn("WL-9001");
        when(conversionMapper.insert(any(CrmDrawingConversion.class))).thenReturn(1);
        when(drawingMapper.updateById(any(CrmDrawing.class))).thenReturn(1);

        CrmMaterial material = new CrmMaterial();
        material.setId(88L);
        material.setMaterialCode("WL-9001");
        when(materialMasterEnsureService.ensureFromDrawing(eq("WL-9001"), anyString())).thenReturn(material);

        CrmBom bom = new CrmBom();
        bom.setId(200L);
        bom.setBomNo("BOM-20260620-E2E1");
        bom.setMaterialCode("WL-9001");
        bom.setStatus("DRAFT");
        bom.setTargetQty(1);
        when(bomService.createBom(any(), eq(1001L))).thenReturn(Result.ok(bom));

        ConversionRequest convReq = new ConversionRequest();
        convReq.setTargetQty(10);
        Result<CrmDrawingConversion> conv = conversionService.convertDrawing(10L, convReq, 1001L);
        assertEquals(0, conv.getCode());
        assertEquals("WL-9001", conv.getData().getMaterialCode());
        assertEquals(200L, conv.getData().getBomId());

        // === Step 2: BOM 子件保存 ===
        when(bomMapper.selectById(200L)).thenReturn(bom);
        when(bomItemMapper.delete(any())).thenReturn(1);
        when(bomItemMapper.insert(any(CrmBomItem.class))).thenReturn(1);
        when(bomMapper.updateById(any(CrmBom.class))).thenReturn(1);

        BomSaveTreeRequest treeReq = new BomSaveTreeRequest();
        treeReq.setBomId(200L);
        List<Map<String, Object>> lines = new ArrayList<>();
        Map<String, Object> line = new HashMap<>();
        line.put("materialCode", "RM-001");
        line.put("materialName", "45#圆钢");
        line.put("qty", 2);
        line.put("unit", "kg");
        line.put("scrapRate", 5);
        lines.add(line);
        treeReq.setLines(lines);
        Result<Map<String, Object>> saveTree = bomServiceReal.saveTree(treeReq);
        assertEquals(0, saveTree.getCode());
        assertTrue((Boolean) saveTree.getData().get("saved"));
        verify(bomItemMapper, times(1)).insert(any(CrmBomItem.class));

        // === Step 3: 工艺路线 RELEASED 过滤（报价预览数据源）===
        Map<String, Object> draftRoute = new HashMap<>();
        draftRoute.put("routeStatus", "DRAFT");
        draftRoute.put("routes", List.of(Map.of("processSeq", 1, "processCode", "P01")));
        when(productRouteClient.getProductRoute("88")).thenReturn(Result.ok(draftRoute));
        Result<List<com.btsheng.erp.business.crm.materialdetail.dto.MaterialDetailDTO.ProcessInfo.ProcessRoute>> draftPreview =
                materialDetailService.getProcessRoute(88L);
        assertTrue(draftPreview.getData().isEmpty(), "草稿路线不应出现在报价预览");

        Map<String, Object> releasedRoute = new HashMap<>();
        releasedRoute.put("routeStatus", "RELEASED");
        releasedRoute.put("routes", List.of(Map.of("processSeq", 1, "processCode", "P00", "isOutsource", false)));
        releasedRoute.put("steps", List.of(Map.of("stepNo", 1, "stepName", "下料", "estimatedHours", 0.08, "machineType", "锯床")));
        when(productRouteClient.getProductRoute("88")).thenReturn(Result.ok(releasedRoute));
        Result<List<com.btsheng.erp.business.crm.materialdetail.dto.MaterialDetailDTO.ProcessInfo.ProcessRoute>> releasedPreview =
                materialDetailService.getProcessRoute(88L);
        assertEquals(0, releasedPreview.getCode());
        assertFalse(releasedPreview.getData().isEmpty(), "已发布路线应可供报价预览");
        assertEquals(1, releasedPreview.getData().get(0).getStepSeq());
    }
}
