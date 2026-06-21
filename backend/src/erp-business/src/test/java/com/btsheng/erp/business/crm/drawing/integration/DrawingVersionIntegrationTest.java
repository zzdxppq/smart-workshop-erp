package com.btsheng.erp.business.crm.drawing.integration;

import com.btsheng.erp.business.crm.drawing.dto.DrawingVersionRequest;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingVersion;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingHistoryMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingVersionMapper;
import com.btsheng.erp.business.crm.drawing.service.DrawingService;
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

/** V1.3.7 Story 1.7 · 版本管理集成测例 (3 测例 · 严格递增 + 跳跃禁止) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DrawingVersionIntegrationTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingVersionMapper versionMapper;
    @Mock private CrmDrawingHistoryMapper historyMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private DrawingService newSvc() {
        return new DrawingService(drawingMapper, versionMapper, historyMapper, docNoGenerator);
    }

    @Test void v1_to_v2_to_v3_full_chain() {
        // 模拟 v1 -> v2 成功
            CrmDrawing d1 = new CrmDrawing();
        d1.setId(1L); d1.setVersion("v1"); d1.setStatus("RELEASED");
        when(drawingMapper.selectById(1L)).thenReturn(d1);
        when(versionMapper.selectMaxVersion(1L)).thenReturn("v1");
        when(versionMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingVersion.class))).thenReturn(1);
        when(drawingMapper.updateById(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        DrawingVersionRequest v2 = new DrawingVersionRequest();
        v2.setVersion("v2");
        Result<CrmDrawingVersion> r2 = svc.addVersion(1L, v2, 1001L);
        assertEquals(0, r2.getCode());

        // 模拟 v2 -> v3 成功
            CrmDrawing d2 = new CrmDrawing();
        d2.setId(1L); d2.setVersion("v2"); d2.setStatus("RELEASED");
        when(drawingMapper.selectById(1L)).thenReturn(d2);
        when(versionMapper.selectMaxVersion(1L)).thenReturn("v2");
        DrawingVersionRequest v3 = new DrawingVersionRequest();
        v3.setVersion("v3");
        Result<CrmDrawingVersion> r3 = svc.addVersion(1L, v3, 1001L);
        assertEquals(0, r3.getCode());
    }

    @Test void skip_version_forbidden_integration() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setVersion("v1"); d.setStatus("RELEASED");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(versionMapper.selectMaxVersion(1L)).thenReturn("v1");
        DrawingService svc = newSvc();
        DrawingVersionRequest req = new DrawingVersionRequest();
        req.setVersion("v3");
        Result<CrmDrawingVersion> r = svc.addVersion(1L, req, 1001L);
        assertEquals(40904, r.getCode());
        assertEquals("VERSION_NOT_STRICTLY_INCREASING", r.getMessage());
    }

    @Test void down_version_forbidden_integration() {
        CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setVersion("v3"); d.setStatus("RELEASED");
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(versionMapper.selectMaxVersion(1L)).thenReturn("v3");
        DrawingService svc = newSvc();
        DrawingVersionRequest req = new DrawingVersionRequest();
        req.setVersion("v2");
        Result<CrmDrawingVersion> r = svc.addVersion(1L, req, 1001L);
        assertEquals(40904, r.getCode());
    }
}
