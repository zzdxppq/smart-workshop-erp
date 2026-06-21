package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.business.crm.drawing.dto.DrawingCreateRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingUpdateRequest;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingHistoryMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingVersionMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** V1.3.7 Story 1.7 · Drawing Controller 集成测例 (3 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DrawingControllerIntegrationTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingVersionMapper versionMapper;
    @Mock private CrmDrawingHistoryMapper historyMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private DrawingService newSvc() {
        return new DrawingService(drawingMapper, versionMapper, historyMapper, docNoGenerator);
    }

    @Test void crud_lifecycle() {
        when(docNoGenerator.nextDrawingNo()).thenReturn("DWG-20260612-0001");
        when(drawingMapper.selectByMaterialCode("WL-1001")).thenReturn(null);
        when(drawingMapper.selectByDrawingNoAndVersion("DWG-20260612-0001", "v1")).thenReturn(null);
        when(drawingMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenAnswer(inv -> {
            ((CrmDrawing) inv.getArgument(0)).setId(1L); return 1;
        });
        when(versionMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingVersion.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        DrawingCreateRequest req = new DrawingCreateRequest();
        req.setTitle("CRUD 测试");
        req.setMaterialCode("WL-1001");
        req.setProcessRoute("[{\"step\":1,\"name\":\"车削\",\"cost\":100}]");
        Result<CrmDrawing> r = svc.createDrawing(req, 1001L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().getId());
    }

    @Test void update_then_get_lifecycle() {
        CrmDrawing existing = new CrmDrawing();
        existing.setId(1L); existing.setStatus("DRAFT");
        existing.setMaterialCode("WL-1001");
        when(drawingMapper.selectById(1L)).thenReturn(existing);
        when(drawingMapper.updateById(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        DrawingUpdateRequest upd = new DrawingUpdateRequest();
        upd.setTitle("Updated Title");
        Result<CrmDrawing> r = svc.updateDrawing(1L, upd, 1001L);
        assertEquals(0, r.getCode());
        // GET
            when(drawingMapper.selectById(1L)).thenReturn(r.getData());
        Result<CrmDrawing> g = svc.getDrawing(1L);
        assertEquals(0, g.getCode());
        assertEquals("Updated Title", g.getData().getTitle());
    }

    @Test void version_release_archive_lifecycle() {
        CrmDrawing d1 = new CrmDrawing();
        d1.setId(1L); d1.setStatus("DRAFT"); d1.setIsFa(0);
        CrmDrawing d2 = new CrmDrawing();
        d2.setId(1L); d2.setStatus("RELEASED"); d2.setIsFa(0);
        when(drawingMapper.selectById(1L)).thenReturn(d1, d2);
        when(drawingMapper.updateById(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        // RELEASE
            Result<CrmDrawing> r1 = svc.releaseDrawing(1L, null, 1001L);
        assertEquals(0, r1.getCode());
        // ARCHIVE
            Result<CrmDrawing> r2 = svc.archiveDrawing(1L, 1001L);
        assertEquals(0, r2.getCode());
        assertEquals("ARCHIVED", r2.getData().getStatus());
    }
}
