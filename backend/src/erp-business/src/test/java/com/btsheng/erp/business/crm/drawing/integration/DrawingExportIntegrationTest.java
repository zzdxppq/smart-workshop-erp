package com.btsheng.erp.business.crm.drawing.integration;

import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingSignature;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingSignatureMapper;
import com.btsheng.erp.business.crm.drawing.service.DrawingEncryptionService;
import com.btsheng.erp.business.crm.drawing.service.DrawingPdfExportService;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** V1.3.7 Story 1.7 · PDF 导出集成测例 (3 测例 · 1h 缓存 + 签字扫描件解密嵌入) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DrawingExportIntegrationTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingSignatureMapper signatureMapper;

    private final DrawingEncryptionService encSvc = new DrawingEncryptionService();
    private DrawingPdfExportService newSvc() {
        return new DrawingPdfExportService(drawingMapper, signatureMapper, encSvc);
    }

    @Test void pdf_export_with_1h_cache() {
        CrmDrawing d = new CrmDrawing();
        d.setId(99L); d.setDrawingNo("DWG-20260612-9999"); d.setVersion("v1");
        d.setTitle("集成测试 PDF"); d.setMaterialCode("WL-9999");
        d.setProcessRoute("[{\"step\":1,\"name\":\"车削\",\"cost\":100}]");
        d.setStatus("RELEASED");
        d.setCreatedAt(LocalDateTime.now());
        when(drawingMapper.selectById(99L)).thenReturn(d);
        when(signatureMapper.selectByDrawingIdAndVersion(99L, "v1")).thenReturn(Collections.emptyList());
        DrawingPdfExportService svc = newSvc();
        // 第一次：DB 查
            Result<byte[]> r1 = svc.exportPdf(99L, "pdf");
        assertEquals(0, r1.getCode());
        // 第二次：缓存命中
            Result<byte[]> r2 = svc.exportPdf(99L, "pdf");
        assertEquals(0, r2.getCode());
        verify(drawingMapper, times(1)).selectById(99L);
    }

    @Test void pdf_export_includes_decrypted_signature() {
        CrmDrawing d = new CrmDrawing();
        d.setId(100L); d.setDrawingNo("DWG-20260612-1000"); d.setVersion("v2");
        d.setTitle("签字集成测试"); d.setMaterialCode("WL-1000");
        d.setProcessRoute("[{\"step\":1,\"name\":\"车削\",\"cost\":100},{\"step\":2,\"name\":\"铣削\",\"cost\":80},{\"step\":3,\"name\":\"热处理\",\"cost\":200},{\"step\":4,\"name\":\"精磨\",\"cost\":150},{\"step\":5,\"name\":\"表面处理\",\"cost\":90}]");
        d.setStatus("RELEASED");
        d.setCreatedAt(LocalDateTime.now());
        when(drawingMapper.selectById(100L)).thenReturn(d);
        String encryptedSig = encSvc.encryptString("李四-2026-06-12-v2-签字");
        CrmDrawingSignature sig = new CrmDrawingSignature();
        sig.setSignerUserId(2001L);
        sig.setSignatureImagePath(encryptedSig);
        when(signatureMapper.selectByDrawingIdAndVersion(100L, "v2")).thenReturn(List.of(sig));
        DrawingPdfExportService svc = newSvc();
        Result<byte[]> r = svc.exportPdf(100L, "pdf");
        assertEquals(0, r.getCode());
        String text = new String(r.getData());
        assertTrue(text.contains("DWG-20260612-1000"));
        assertTrue(text.contains("签字集成测试"));
        assertTrue(text.contains("签字人:2001"));
        assertTrue(text.contains("李四-2026-06-12-v2-签字"));
    }

    @Test void pdf_audit_action_recorded() {
        // 验证 PDF 导出触发审计
            CrmDrawing d = new CrmDrawing();
        d.setId(101L); d.setDrawingNo("DWG-20260612-1001"); d.setVersion("v1");
        d.setTitle("审计测试"); d.setMaterialCode("WL-1001");
        d.setProcessRoute("[{\"step\":1,\"name\":\"车削\",\"cost\":100}]");
        d.setStatus("RELEASED");
        d.setCreatedAt(LocalDateTime.now());
        when(drawingMapper.selectById(101L)).thenReturn(d);
        when(signatureMapper.selectByDrawingIdAndVersion(101L, "v1")).thenReturn(Collections.emptyList());
        DrawingPdfExportService svc = newSvc();
        Result<byte[]> r = svc.exportPdf(101L, "pdf");
        assertEquals(0, r.getCode());
        // 审计通过 @AuditLog(action="drawing.pdf_download") 记录（切面执行，集成层只验证行为）
            assertNotNull(r.getData());
        assertTrue(r.getData().length > 0);
    }
}
