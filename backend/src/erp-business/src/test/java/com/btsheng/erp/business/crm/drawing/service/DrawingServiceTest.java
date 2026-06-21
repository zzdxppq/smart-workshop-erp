package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.business.crm.drawing.dto.DrawingCreateRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingQueryRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingReleaseRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingUpdateRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingVersionRequest;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** V1.3.7 Story 1.7 · AC-3.1.1/2/3 · DrawingService 单元测例 (8 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DrawingServiceTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingVersionMapper versionMapper;
    @Mock private CrmDrawingHistoryMapper historyMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private DrawingService newSvc() {
        return new DrawingService(drawingMapper, versionMapper, historyMapper, docNoGenerator);
    }

    private DrawingCreateRequest validReq() {
        DrawingCreateRequest r = new DrawingCreateRequest();
        r.setTitle("测试图纸");
        r.setMaterialCode("WL-9999");
        r.setProcessRoute("[{\"step\":1,\"name\":\"车削\",\"cost\":100.00}]");
        r.setIsFa(0);
        r.setIsNew(1);
        r.setPdfPath("/data/pdf/test.pdf");
        return r;
    }

    @Test void create_drawing_success() {
        when(docNoGenerator.nextDrawingNo()).thenReturn("DWG-20260612-0001");
        when(drawingMapper.selectByMaterialCode("WL-9999")).thenReturn(null);
        when(drawingMapper.selectByDrawingNoAndVersion("DWG-20260612-0001", "v1")).thenReturn(null);
        when(drawingMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenAnswer(inv -> {
            ((CrmDrawing) inv.getArgument(0)).setId(1L); return 1;
        });
        when(versionMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingVersion.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        Result<CrmDrawing> r = svc.createDrawing(validReq(), 1001L);
        assertEquals(0, r.getCode());
        assertEquals("DWG-20260612-0001", r.getData().getDrawingNo());
        assertEquals("v1", r.getData().getVersion());
        assertEquals("DRAFT", r.getData().getStatus());
    }

    @Test void create_drawing_material_code_format_invalid() {
        DrawingService svc = newSvc();
        DrawingCreateRequest req = validReq();
        req.setMaterialCode("INVALID");
        Result<CrmDrawing> r = svc.createDrawing(req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void create_drawing_duplicate_material_code() {
        CrmDrawing existing = new CrmDrawing();
        existing.setId(99L); existing.setMaterialCode("WL-9999");
        when(drawingMapper.selectByMaterialCode("WL-9999")).thenReturn(existing);
        DrawingService svc = newSvc();
        Result<CrmDrawing> r = svc.createDrawing(validReq(), 1001L);
        assertEquals(40905, r.getCode());
    }

    @Test void get_drawing_not_found() {
        when(drawingMapper.selectById(99L)).thenReturn(null);
        DrawingService svc = newSvc();
        Result<CrmDrawing> r = svc.getDrawing(99L);
        assertEquals(40404, r.getCode());
    }

    @Test void update_drawing_not_editable() {
        CrmDrawing existing = new CrmDrawing();
        existing.setId(1L); existing.setStatus("RELEASED");
        when(drawingMapper.selectById(1L)).thenReturn(existing);
        DrawingService svc = newSvc();
        DrawingUpdateRequest req = new DrawingUpdateRequest();
        req.setTitle("Updated");
        Result<CrmDrawing> r = svc.updateDrawing(1L, req, 1001L);
        assertEquals(40903, r.getCode());
    }

    @Test void list_drawings_6d() {
        when(drawingMapper.selectDrawings6D(any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(drawingMapper.countDrawings6D(any(), any(), any(), any())).thenReturn(0L);
        DrawingService svc = newSvc();
        DrawingQueryRequest q = new DrawingQueryRequest();
        q.setPage(0); q.setSize(20); q.setStatus("DRAFT");
        Result<Map<String, Object>> r = svc.listDrawings(q);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().containsKey("list"));
        assertTrue(r.getData().containsKey("total"));
    }

    @Test void release_drawing_state_invalid() {
        CrmDrawing existing = new CrmDrawing();
        existing.setId(1L); existing.setStatus("RELEASED");
        when(drawingMapper.selectById(1L)).thenReturn(existing);
        DrawingService svc = newSvc();
        Result<CrmDrawing> r = svc.releaseDrawing(1L, new DrawingReleaseRequest(), 1001L);
        assertEquals(40904, r.getCode());
    }

    @Test void archive_drawing_state_invalid() {
        CrmDrawing existing = new CrmDrawing();
        existing.setId(1L); existing.setStatus("DRAFT");
        when(drawingMapper.selectById(1L)).thenReturn(existing);
        DrawingService svc = newSvc();
        Result<CrmDrawing> r = svc.archiveDrawing(1L, 1001L);
        assertEquals(40904, r.getCode());
    }
}
