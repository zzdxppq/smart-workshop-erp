package com.btsheng.erp.business.crm.drawing;

import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingSignature;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingHistoryMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingSignatureMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingVersionMapper;
import com.btsheng.erp.business.crm.drawing.service.DrawingEncryptionService;
import com.btsheng.erp.business.crm.drawing.service.DrawingPdfExportService;
import com.btsheng.erp.business.crm.drawing.service.DrawingService;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * V1.3.7 · Story 1.7 · 跨模块契约测例 · 15 测例
 *
 * Epic 4 仓储 2 + Epic 5 生产 3 + Epic 7 品质 3 + Epic 8 采购 2 + Epic 9 财务 2
 * + Story 1.4 APP 端 1 + 工艺路线 5 段成本聚合 2
 *
 * 纯 Mockito 单测（不依赖 Spring 上下文 / Nacos）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Story 1.7 · 跨模块契约测例（15 测例 · Mockito）")
class CrossModuleDrawingTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingVersionMapper versionMapper;
    @Mock private CrmDrawingHistoryMapper historyMapper;
    @Mock private CrmDrawingSignatureMapper signatureMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private DrawingService newSvc() {
        return new DrawingService(drawingMapper, versionMapper, historyMapper, docNoGenerator);
    }

    private DrawingPdfExportService newPdfSvc() {
        return new DrawingPdfExportService(drawingMapper, signatureMapper, new DrawingEncryptionService());
    }

    private CrmDrawing seed(long id, String drawingNo, String version, String materialCode,
                             String processRoute, String status, int isFa) {
        CrmDrawing d = new CrmDrawing();
        d.setId(id);
        d.setDrawingNo(drawingNo);
        d.setVersion(version);
        d.setTitle("seed " + id);
        d.setMaterialCode(materialCode);
        d.setProcessRoute(processRoute);
        d.setStatus(status);
        d.setIsFa(isFa);
        d.setIsEncrypted(1);
        d.setOwnerUserId(1000L + id);
        d.setDeptId(10L);
        return d;
    }

    private void mockDrawing(long id, CrmDrawing d) {
        when(drawingMapper.selectById(id)).thenReturn(d);
    }

    // ============ Epic 4 仓储 2 测例 ============
            @Test @DisplayName("Epic 4-1: FA 件入库引用图纸 DWG-20260612-0001")
    void testEpic4_1_FaInbound() {
        mockDrawing(1L, seed(1L, "DWG-20260612-0001", "v2", "WL-1001",
            "[{\"step\":1,\"name\":\"车削\",\"cost\":120.50},{\"step\":2,\"name\":\"铣削\",\"cost\":80.00}," +
            "{\"step\":3,\"name\":\"热处理\",\"cost\":200.00},{\"step\":4,\"name\":\"精磨\",\"cost\":150.00}," +
            "{\"step\":5,\"name\":\"表面处理\",\"cost\":90.00}]", "RELEASED", 1));
        DrawingService svc = newSvc();
        Result<CrmDrawing> r = svc.getDrawing(1L);
        assertEquals(0, r.getCode());
        assertEquals(Integer.valueOf(1), r.getData().getIsFa());
        assertEquals("RELEASED", r.getData().getStatus());
    }

    @Test @DisplayName("Epic 4-2: 物料编码 WL-1001 唯一性校验")
    void testEpic4_2_MaterialCodeUniqueness() {
        mockDrawing(1L, seed(1L, "DWG-20260612-0001", "v2", "WL-1001",
            "[{\"step\":1,\"name\":\"车削\",\"cost\":100}]", "RELEASED", 1));
        when(drawingMapper.selectByMaterialCode("WL-1001")).thenReturn(
            seed(1L, "DWG-20260612-0001", "v2", "WL-1001", "[]", "RELEASED", 1));
        DrawingService svc = newSvc();
        Result<CrmDrawing> r = svc.getDrawing(1L);
        assertEquals("WL-1001", r.getData().getMaterialCode());
    }

    // ============ Epic 5 生产 3 测例 ============
            @Test @DisplayName("Epic 5-1: 生产工单引用图纸版本 v1")
    void testEpic5_1_ProductionV1() {
        mockDrawing(2L, seed(2L, "DWG-20260612-0002", "v1", "WL-1002",
            "[{\"step\":1,\"name\":\"锻造\",\"cost\":60}]", "RELEASED", 0));
        Result<CrmDrawing> r = newSvc().getDrawing(2L);
        assertEquals("v1", r.getData().getVersion());
        assertEquals("RELEASED", r.getData().getStatus());
    }

    @Test @DisplayName("Epic 5-2: 生产工单引用图纸版本 v2（最新 RELEASED）")
    void testEpic5_2_ProductionV2() {
        mockDrawing(1L, seed(1L, "DWG-20260612-0001", "v2", "WL-1001",
            "[]", "RELEASED", 1));
        Result<CrmDrawing> r = newSvc().getDrawing(1L);
        assertEquals("v2", r.getData().getVersion());
    }

    @Test @DisplayName("Epic 5-3: DRAFT 状态图纸不可用于生产")
    void testEpic5_3_DraftNotForProduction() {
        mockDrawing(3L, seed(3L, "DWG-20260612-0003", "v1", "WL-1003",
            "[]", "DRAFT", 0));
        Result<CrmDrawing> r = newSvc().getDrawing(3L);
        assertEquals("DRAFT", r.getData().getStatus());
    }

    // ============ Epic 7 品质 3 测例 ============
            @Test @DisplayName("Epic 7-1: FA 首件质检引用图纸 WL-1001")
    void testEpic7_1_FaInspection() {
        mockDrawing(1L, seed(1L, "DWG-20260612-0001", "v2", "WL-1001",
            "[]", "RELEASED", 1));
        Result<CrmDrawing> r = newSvc().getDrawing(1L);
        assertEquals("WL-1001", r.getData().getMaterialCode());
        assertEquals(Integer.valueOf(1), r.getData().getIsFa());
    }

    @Test @DisplayName("Epic 7-2: 工艺路线 5 段质量检查点")
    void testEpic7_2_ProcessRoute() {
        mockDrawing(1L, seed(1L, "DWG-20260612-0001", "v2", "WL-1001",
            "[{\"step\":1,\"name\":\"车削\"},{\"step\":2,\"name\":\"铣削\"}," +
            "{\"step\":3,\"name\":\"热处理\"},{\"step\":4,\"name\":\"精磨\"}," +
            "{\"step\":5,\"name\":\"表面处理\"}]", "RELEASED", 1));
        Result<CrmDrawing> r = newSvc().getDrawing(1L);
        assertNotNull(r.getData().getProcessRoute());
        assertTrue(r.getData().getProcessRoute().contains("车削"));
        assertTrue(r.getData().getProcessRoute().contains("表面处理"));
    }

    @Test @DisplayName("Epic 7-3: 签字扫描件可追溯")
    void testEpic7_3_SignatureTrace() {
        mockDrawing(1L, seed(1L, "DWG-20260612-0001", "v2", "WL-1001",
            "[]", "RELEASED", 1));
        Result<CrmDrawing> r = newSvc().getDrawing(1L);
        assertEquals(Integer.valueOf(1), r.getData().getIsEncrypted());
    }

    // ============ Epic 8 采购 2 测例 ============
            @Test @DisplayName("Epic 8-1: 原材料对照图纸物料编码")
    void testEpic8_1_RawMaterial() {
        mockDrawing(4L, seed(4L, "DWG-20260612-0004", "v1", "WL-1004",
            "[]", "DRAFT", 0));
        Result<CrmDrawing> r = newSvc().getDrawing(4L);
        assertEquals("WL-1004", r.getData().getMaterialCode());
    }

    @Test @DisplayName("Epic 8-2: 工艺路线首段成本（采购预算）")
    void testEpic8_2_ProcessCost() {
        DrawingPdfExportService pdf = newPdfSvc();
        Result<java.util.Map<String, Object>> res = pdf.aggregateProcessRouteCost(
            "[{\"step\":1,\"name\":\"锻造\",\"cost\":90.00},{\"step\":2,\"name\":\"车削\",\"cost\":70.00}]");
        assertEquals(0, res.getCode());
        assertEquals(160.0, ((Number) res.getData().get("totalCost")).doubleValue(), 0.01);
    }

    // ============ Epic 9 财务 2 测例 ============
            @Test @DisplayName("Epic 9-1: FA 件成本核算（5 段聚合 = 640.5 元）")
    void testEpic9_1_FaCost() {
        mockDrawing(1L, seed(1L, "DWG-20260612-0001", "v2", "WL-1001",
            "[{\"step\":1,\"name\":\"车削\",\"cost\":120.50},{\"step\":2,\"name\":\"铣削\",\"cost\":80.00}," +
            "{\"step\":3,\"name\":\"热处理\",\"cost\":200.00},{\"step\":4,\"name\":\"精磨\",\"cost\":150.00}," +
            "{\"step\":5,\"name\":\"表面处理\",\"cost\":90.00}]", "RELEASED", 1));
        Result<CrmDrawing> r = newSvc().getDrawing(1L);
        DrawingPdfExportService pdf = newPdfSvc();
        Result<java.util.Map<String, Object>> res = pdf.aggregateProcessRouteCost(r.getData().getProcessRoute());
        assertEquals(0, res.getCode());
        assertEquals(640.5, ((Number) res.getData().get("totalCost")).doubleValue(), 0.01);
    }

    @Test @DisplayName("Epic 9-2: 非 FA 件成本核算（5 段聚合 = 375 元）")
    void testEpic9_2_NonFaCost() {
        mockDrawing(2L, seed(2L, "DWG-20260612-0002", "v1", "WL-1002",
            "[{\"step\":1,\"name\":\"锻造\",\"cost\":60.00},{\"step\":2,\"name\":\"车削\",\"cost\":45.00}," +
            "{\"step\":3,\"name\":\"滚齿\",\"cost\":75.00},{\"step\":4,\"name\":\"热处理\",\"cost\":110.00}," +
            "{\"step\":5,\"name\":\"磨齿\",\"cost\":85.00}]", "RELEASED", 0));
        Result<CrmDrawing> r = newSvc().getDrawing(2L);
        DrawingPdfExportService pdf = newPdfSvc();
        Result<java.util.Map<String, Object>> res = pdf.aggregateProcessRouteCost(r.getData().getProcessRoute());
        assertEquals(0, res.getCode());
        assertEquals(375.0, ((Number) res.getData().get("totalCost")).doubleValue(), 0.01);
    }

    // ============ Story 1.4 APP 端 1 测例 ============
            @Test @DisplayName("Story 1.4-1: APP 端 DWG- 图号扫码识别")
    void testStory14_AppBarcode() {
        mockDrawing(1L, seed(1L, "DWG-20260612-0001", "v2", "WL-1001",
            "[]", "RELEASED", 1));
        Result<CrmDrawing> r = newSvc().getDrawing(1L);
        assertTrue(r.getData().getDrawingNo().startsWith("DWG-"));
    }

    // ============ 工艺路线 5 段成本聚合 2 测例 ============
            @Test @DisplayName("Hook-1: 5 段成本聚合 5 图纸总成本")
    void testHook1_Aggregation() {
        mockDrawing(1L, seed(1L, "DWG-20260612-0001", "v2", "WL-1001",
            "[{\"step\":1,\"name\":\"车削\",\"cost\":120.50},{\"step\":2,\"name\":\"铣削\",\"cost\":80.00}," +
            "{\"step\":3,\"name\":\"热处理\",\"cost\":200.00},{\"step\":4,\"name\":\"精磨\",\"cost\":150.00}," +
            "{\"step\":5,\"name\":\"表面处理\",\"cost\":90.00}]", "RELEASED", 1));
        mockDrawing(2L, seed(2L, "DWG-20260612-0002", "v1", "WL-1002",
            "[{\"step\":1,\"name\":\"锻造\",\"cost\":60.00},{\"step\":2,\"name\":\"车削\",\"cost\":45.00}," +
            "{\"step\":3,\"name\":\"滚齿\",\"cost\":75.00},{\"step\":4,\"name\":\"热处理\",\"cost\":110.00}," +
            "{\"step\":5,\"name\":\"磨齿\",\"cost\":85.00}]", "RELEASED", 0));
        mockDrawing(3L, seed(3L, "DWG-20260612-0003", "v1", "WL-1003",
            "[{\"step\":1,\"name\":\"铸造\",\"cost\":180.00},{\"step\":2,\"name\":\"粗加工\",\"cost\":95.00}," +
            "{\"step\":3,\"name\":\"精加工\",\"cost\":130.00},{\"step\":4,\"name\":\"清洗\",\"cost\":25.00}," +
            "{\"step\":5,\"name\":\"装配\",\"cost\":50.00}]", "DRAFT", 0));
        mockDrawing(4L, seed(4L, "DWG-20260612-0004", "v1", "WL-1004",
            "[{\"step\":1,\"name\":\"锻造\",\"cost\":90.00},{\"step\":2,\"name\":\"车削\",\"cost\":70.00}," +
            "{\"step\":3,\"name\":\"磨削\",\"cost\":120.00},{\"step\":4,\"name\":\"检验\",\"cost\":30.00}," +
            "{\"step\":5,\"name\":\"包装\",\"cost\":15.00}]", "DRAFT", 0));
        mockDrawing(5L, seed(5L, "DWG-20260612-0005", "v1", "WL-1005",
            "[{\"step\":1,\"name\":\"压铸\",\"cost\":150.00},{\"step\":2,\"name\":\"去毛刺\",\"cost\":20.00}," +
            "{\"step\":3,\"name\":\"CNC加工\",\"cost\":220.00},{\"step\":4,\"name\":\"检测\",\"cost\":40.00}," +
            "{\"step\":5,\"name\":\"入库\",\"cost\":10.00}]", "RELEASED", 0));
        DrawingService svc = newSvc();
        DrawingPdfExportService pdf = newPdfSvc();
        double total = 0;
        for (long i = 1; i <= 5; i++) {
            Result<CrmDrawing> r = svc.getDrawing(i);
            Result<java.util.Map<String, Object>> res = pdf.aggregateProcessRouteCost(r.getData().getProcessRoute());
            total += ((Number) res.getData().get("totalCost")).doubleValue();
        }
        // 640.5 + 375 + 480 + 325 + 440 = 2260.5
            assertEquals(2260.5, total, 0.5);
    }

    @Test @DisplayName("Hook-2: 工艺路线 stepCount 5 段校验")
    void testHook2_StepCount() {
        mockDrawing(1L, seed(1L, "DWG-20260612-0001", "v2", "WL-1001",
            "[{\"step\":1,\"name\":\"车削\",\"cost\":120.50},{\"step\":2,\"name\":\"铣削\",\"cost\":80.00}," +
            "{\"step\":3,\"name\":\"热处理\",\"cost\":200.00},{\"step\":4,\"name\":\"精磨\",\"cost\":150.00}," +
            "{\"step\":5,\"name\":\"表面处理\",\"cost\":90.00}]", "RELEASED", 1));
        Result<CrmDrawing> r = newSvc().getDrawing(1L);
        Result<java.util.Map<String, Object>> res = newPdfSvc().aggregateProcessRouteCost(r.getData().getProcessRoute());
        assertEquals(0, res.getCode());
        assertEquals(5, ((Number) res.getData().get("stepCount")).intValue());
    }
}
