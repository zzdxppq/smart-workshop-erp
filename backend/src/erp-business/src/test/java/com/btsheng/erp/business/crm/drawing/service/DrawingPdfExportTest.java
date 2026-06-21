package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingSignature;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingSignatureMapper;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** V1.3.7 Story 1.7 · AC-3.1.4 · PDF 导出 单元测例 (3 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DrawingPdfExportTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingSignatureMapper signatureMapper;

    private DrawingEncryptionService encSvc = new DrawingEncryptionService();
    private DrawingPdfExportService newSvc() {
        return new DrawingPdfExportService(drawingMapper, signatureMapper, encSvc);
    }

    @Test void export_pdf_1h_cache_hit() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setDrawingNo("DWG-20260612-0001"); d.setVersion("v1");
        d.setTitle("测试图纸"); d.setMaterialCode("WL-1001");
        d.setProcessRoute("[{\"step\":1,\"name\":\"车削\",\"cost\":100}]");
        d.setStatus("RELEASED");
        d.setCreatedAt(LocalDateTime.now());
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(signatureMapper.selectByDrawingIdAndVersion(1L, "v1")).thenReturn(Collections.emptyList());
        DrawingPdfExportService svc = newSvc();
        Result<byte[]> r1 = svc.exportPdf(1L, "pdf");
        Result<byte[]> r2 = svc.exportPdf(1L, "pdf");
        assertEquals(0, r1.getCode());
        assertEquals(0, r2.getCode());
        // 缓存命中只查 1 次
            verify(drawingMapper, times(1)).selectById(1L);
    }

    @Test void export_pdf_includes_signature_decrypted() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setDrawingNo("DWG-20260612-0001"); d.setVersion("v1");
        d.setTitle("签字图纸"); d.setMaterialCode("WL-1001");
        d.setProcessRoute("[{\"step\":1,\"name\":\"车削\",\"cost\":100}]");
        d.setStatus("RELEASED");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        String encryptedSig = encSvc.encryptString("张三-2026-06-12");
        CrmDrawingSignature sig = new CrmDrawingSignature();
        sig.setSignerUserId(1001L);
        sig.setSignatureImagePath(encryptedSig);
        when(signatureMapper.selectByDrawingIdAndVersion(1L, "v1")).thenReturn(java.util.List.of(sig));
        DrawingPdfExportService svc = newSvc();
        Result<byte[]> r = svc.exportPdf(1L, "pdf");
        assertEquals(0, r.getCode());
        String text = new String(r.getData());
        assertTrue(text.contains("DWG-20260612-0001"));
        assertTrue(text.contains("签字图纸"));
    }

    @Test void aggregate_process_route_cost_5_steps() {
        DrawingPdfExportService svc = newSvc();
        String json = "[{\"step\":1,\"name\":\"车削\",\"cost\":100.5},"
                    + "{\"step\":2,\"name\":\"铣削\",\"cost\":80.0},"
                    + "{\"step\":3,\"name\":\"热处理\",\"cost\":200.0},"
                    + "{\"step\":4,\"name\":\"精磨\",\"cost\":150.0},"
                    + "{\"step\":5,\"name\":\"表面处理\",\"cost\":90.0}]";
        Result<Map<String, Object>> r = svc.aggregateProcessRouteCost(json);
        assertEquals(0, r.getCode());
        assertEquals(5, r.getData().get("stepCount"));
        assertEquals(620.5, ((Number) r.getData().get("totalCost")).doubleValue(), 0.01);
        assertEquals("V1.3.4-cost-aggregator-ready-for-1.9-bom", r.getData().get("hook"));
    }
}
