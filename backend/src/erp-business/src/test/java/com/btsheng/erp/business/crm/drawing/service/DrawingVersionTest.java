package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.business.crm.drawing.dto.DrawingVersionRequest;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingVersion;
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

/** V1.3.7 Story 1.7 · AC-3.1.2 · DrawingVersion 单元测例 (5 测例 · P1 修补) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DrawingVersionTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingVersionMapper versionMapper;
    @Mock private CrmDrawingHistoryMapper historyMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private DrawingService newSvc() {
        return new DrawingService(drawingMapper, versionMapper, historyMapper, docNoGenerator);
    }

    @Test void v1_to_v2_strictly_increasing() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setVersion("v1"); d.setStatus("RELEASED");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(versionMapper.selectMaxVersion(1L)).thenReturn("v1");
        when(versionMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingVersion.class))).thenReturn(1);
        when(drawingMapper.updateById(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        DrawingVersionRequest req = new DrawingVersionRequest();
        req.setVersion("v2"); req.setChangeReason("v2 upgrade");
        Result<CrmDrawingVersion> r = svc.addVersion(1L, req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void v2_to_v3_strictly_increasing() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setVersion("v2"); d.setStatus("RELEASED");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(versionMapper.selectMaxVersion(1L)).thenReturn("v2");
        when(versionMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingVersion.class))).thenReturn(1);
        when(drawingMapper.updateById(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        DrawingVersionRequest req = new DrawingVersionRequest();
        req.setVersion("v3");
        Result<CrmDrawingVersion> r = svc.addVersion(1L, req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void skip_v1_to_v3_forbidden() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setVersion("v1"); d.setStatus("RELEASED");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(versionMapper.selectMaxVersion(1L)).thenReturn("v1");
        DrawingService svc = newSvc();
        DrawingVersionRequest req = new DrawingVersionRequest();
        req.setVersion("v3");
        Result<CrmDrawingVersion> r = svc.addVersion(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }

    @Test void v1_to_v1_forbidden_not_strictly_increasing() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setVersion("v1"); d.setStatus("RELEASED");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(versionMapper.selectMaxVersion(1L)).thenReturn("v1");
        DrawingService svc = newSvc();
        DrawingVersionRequest req = new DrawingVersionRequest();
        req.setVersion("v1");
        Result<CrmDrawingVersion> r = svc.addVersion(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }

    @Test void archived_state_reject_new_version() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setVersion("v2"); d.setStatus("ARCHIVED");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        DrawingService svc = newSvc();
        DrawingVersionRequest req = new DrawingVersionRequest();
        req.setVersion("v3");
        Result<CrmDrawingVersion> r = svc.addVersion(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }
}
