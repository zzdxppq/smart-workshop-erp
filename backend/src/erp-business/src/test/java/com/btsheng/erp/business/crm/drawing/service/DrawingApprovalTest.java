package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.business.crm.drawing.dto.DrawingReleaseRequest;
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

/** V1.3.7 Story 1.7 · AC-3.1.3 · 4 阈值路由 + 二次密码（4 测例） */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DrawingApprovalTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingVersionMapper versionMapper;
    @Mock private CrmDrawingHistoryMapper historyMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private DrawingService newSvc() {
        return new DrawingService(drawingMapper, versionMapper, historyMapper, docNoGenerator);
    }

    @Test void non_fa_piece_release_ok() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setStatus("DRAFT"); d.setIsFa(0);
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(drawingMapper.updateById(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        Result<CrmDrawing> r = svc.releaseDrawing(1L, new DrawingReleaseRequest(), 1001L);
        assertEquals(0, r.getCode());
        assertEquals("RELEASED", r.getData().getStatus());
    }

    @Test void fa_piece_requires_admin_password() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setStatus("DRAFT"); d.setIsFa(1);
        when(drawingMapper.selectById(1L)).thenReturn(d);
        DrawingService svc = newSvc();
        DrawingReleaseRequest req = new DrawingReleaseRequest();
        // 未提供 adminPassword
            Result<CrmDrawing> r = svc.releaseDrawing(1L, req, 1001L);
        assertEquals(40101, r.getCode());
    }

    @Test void fa_piece_with_admin_password_ok() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setStatus("DRAFT"); d.setIsFa(1);
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(drawingMapper.updateById(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        DrawingReleaseRequest req = new DrawingReleaseRequest();
        req.setAdminPassword("admin_pwd_2026");
        Result<CrmDrawing> r = svc.releaseDrawing(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals("RELEASED", r.getData().getStatus());
    }

    @Test void state_machine_guard_blocks_double_release() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setStatus("ARCHIVED");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        DrawingService svc = newSvc();
        DrawingReleaseRequest req = new DrawingReleaseRequest();
        req.setAdminPassword("admin_pwd");
        Result<CrmDrawing> r = svc.releaseDrawing(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }
}
