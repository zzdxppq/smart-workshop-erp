package com.btsheng.erp.business.crm.drawing.integration;

import com.btsheng.erp.business.crm.drawing.authz.DrawingAuthz;
import com.btsheng.erp.business.crm.drawing.dto.DrawingPermissionDTO;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingLink;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingLinkMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingSignatureMapper;
import com.btsheng.erp.business.crm.drawing.service.DrawingEncryptionService;
import com.btsheng.erp.business.crm.drawing.service.DrawingPdfExportService;
import com.btsheng.erp.business.crm.drawing.service.DrawingPreviewService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.business.platform.mapper.PlatformLookupMapper;
import com.btsheng.erp.core.redis.CacheTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * V1.3.9 Sprint 12 Story 12.1 · 端点集成测试（6 测例 · QA 测例组 2）
 *
 * <ul>
 *   <li>TC-12.1.2.1 SALES 关联订单 → permission 200 + view=true + scope=ORDER</li>
 *   <li>TC-12.1.2.2 FINANCE → permission 200 + 全 false + scope=NONE（不返 403）</li>
 *   <li>TC-12.1.2.3 ENGINEER → preview 200 + PDF 流 + 水印</li>
 *   <li>TC-12.1.2.4 SALES 不关联订单 → preview 40304 + message</li>
 *   <li>TC-12.1.2.5 ENGINEER → download 200 + 流</li>
 *   <li>TC-12.1.2.6 SALES → download 40304</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DrawingAclEndpointIntegrationTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingSignatureMapper signatureMapper;
    @Mock private CrmDrawingLinkMapper linkMapper;
    @Mock private CacheTemplate cacheTemplate;
    @Mock private PlatformLookupMapper platformLookupMapper;

    private final DrawingEncryptionService encSvc = new DrawingEncryptionService();

    private DrawingPreviewService newSvc() {
        DrawingPdfExportService pdfSvc = new DrawingPdfExportService(drawingMapper, signatureMapper, encSvc);
        DrawingAuthz authz = new DrawingAuthz(linkMapper, cacheTemplate, platformLookupMapper);
        return new DrawingPreviewService(drawingMapper, pdfSvc, authz);
    }

    @Test
    void permission_SALES_linked_ok() {
        when(linkMapper.selectBizIdsByDrawingAndBizType(789L, CrmDrawingLink.BIZ_TYPE_ORDER))
                .thenReturn(List.of(100L));
        when(linkMapper.selectBizIdsByDrawingAndBizType(789L, CrmDrawingLink.BIZ_TYPE_PO))
                .thenReturn(Collections.emptyList());
        when(linkMapper.selectBizIdsByDrawingAndBizType(789L, CrmDrawingLink.BIZ_TYPE_INCOMING))
                .thenReturn(Collections.emptyList());
        when(linkMapper.selectBizIdsByDrawingAndBizType(789L, CrmDrawingLink.BIZ_TYPE_INSPECTION))
                .thenReturn(Collections.emptyList());
        when(linkMapper.selectBizIdsByDrawingAndBizType(789L, CrmDrawingLink.BIZ_TYPE_WORKORDER_PROCESS))
                .thenReturn(Collections.emptyList());
        CrmDrawing d = newDrawing(789L, "DWG-20260614-0001");
        when(drawingMapper.selectById(789L)).thenReturn(d);

        DrawingPreviewService svc = newSvc();
        Result<DrawingPermissionDTO> r = svc.getPermission(789L, auth("sales01", "ROLE_SALES"));
        assertEquals(0, r.getCode());
        DrawingPermissionDTO dto = r.getData();
        assertEquals("SALES", dto.getRole());
        assertEquals("ORDER", dto.getScope());
        assertTrue(dto.getPermissions().isView());
        assertTrue(dto.getPermissions().isPrint());
        assertFalse(dto.getPermissions().isDownload());
        assertFalse(dto.getPermissions().isUpload());
        assertFalse(dto.getPermissions().isDelete());
        assertNotNull(dto.getLinkedBizIds());
        assertEquals(List.of(100L), dto.getLinkedBizIds().get("ORDER"));
    }

    @Test
    void permission_FINANCE_ok_200_no_403() {
        for (String bt : new String[]{"ORDER", "PO", "INCOMING", "INSPECTION", "WORKORDER_PROCESS"}) {
            when(linkMapper.selectBizIdsByDrawingAndBizType(789L, bt))
                    .thenReturn(Collections.emptyList());
        }
        CrmDrawing d = newDrawing(789L, "DWG-20260614-0002");
        when(drawingMapper.selectById(789L)).thenReturn(d);

        DrawingPreviewService svc = newSvc();
        Result<DrawingPermissionDTO> r = svc.getPermission(789L, auth("finance01", "ROLE_FINANCE"));
        assertEquals(0, r.getCode());
        DrawingPermissionDTO dto = r.getData();
        assertEquals("FINANCE", dto.getRole());
        assertEquals("NONE", dto.getScope());
        assertFalse(dto.getPermissions().isView());
        assertFalse(dto.getPermissions().isPrint());
        assertFalse(dto.getPermissions().isDownload());
    }

    @Test
    void preview_ENGINEER_pdf_with_watermark() {
        CrmDrawing d = newDrawing(100L, "DWG-20260614-0003");
        when(drawingMapper.selectById(100L)).thenReturn(d);

        DrawingPreviewService svc = newSvc();
        Result<byte[]> r = svc.previewPDF(100L, "MEDIUM", auth("engineer01", "ROLE_ENGINEER"));
        assertEquals(0, r.getCode());
        assertNotNull(r.getData());
        String text = new String(r.getData());
        assertTrue(text.contains("WATERMARK"), "水印缺失");
        assertTrue(text.contains("engineer01"));
        assertTrue(text.contains("DWG-20260614-0003"));
    }

    @Test
    void preview_SALES_unlinked_40304() {
        CrmDrawing d = newDrawing(789L, "DWG-20260614-0004");
        when(drawingMapper.selectById(789L)).thenReturn(d);
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(anyLong(), any(), any())).thenReturn(0L);

        DrawingPreviewService svc = newSvc();
        Result<byte[]> r = svc.previewPDF(789L, "MEDIUM", auth("sales01", "ROLE_SALES"));
        assertEquals(40304, r.getCode());
        assertTrue(r.getMessage().contains("订单"));
    }

    @Test
    void download_ENGINEER_ok() {
        CrmDrawing d = newDrawing(100L, "DWG-20260614-0005");
        when(drawingMapper.selectById(100L)).thenReturn(d);

        DrawingPreviewService svc = newSvc();
        Result<byte[]> r = svc.downloadOriginal(100L, auth("engineer01", "ROLE_ENGINEER"));
        assertEquals(0, r.getCode());
        assertNotNull(r.getData());
    }

    @Test
    void download_SALES_40304() {
        CrmDrawing d = newDrawing(789L, "DWG-20260614-0006");
        when(drawingMapper.selectById(789L)).thenReturn(d);

        DrawingPreviewService svc = newSvc();
        Result<byte[]> r = svc.downloadOriginal(789L, auth("sales01", "ROLE_SALES"));
        assertEquals(40304, r.getCode());
        assertTrue(r.getMessage().contains("ENGINEER"));
    }

    // ============================================================
    // 辅助
    // ============================================================
            private Authentication auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(username, "password",
                Collections.singletonList(new SimpleGrantedAuthority(role)));
    }

    private CrmDrawing newDrawing(Long id, String drawingNo) {
        CrmDrawing d = new CrmDrawing();
        d.setId(id);
        d.setDrawingNo(drawingNo);
        d.setVersion("v1");
        d.setTitle("集成测试图纸");
        d.setMaterialCode("WL-9999");
        d.setProcessRoute("[{\"step\":1,\"name\":\"车削\",\"cost\":100}]");
        d.setStatus("RELEASED");
        d.setCreatedAt(LocalDateTime.now());
        return d;
    }
}