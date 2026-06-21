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

/** V1.3.7 Story 1.7 · 物料编码 WL-XXXX 校验 (3 测例 · P1 修补) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DrawingMaterialCodeTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingVersionMapper versionMapper;
    @Mock private CrmDrawingHistoryMapper historyMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private DrawingService newSvc() {
        return new DrawingService(drawingMapper, versionMapper, historyMapper, docNoGenerator);
    }

    @Test void valid_wl_xxxx_format_accepted() {
        when(docNoGenerator.nextDrawingNo()).thenReturn("DWG-20260612-0001");
        when(drawingMapper.selectByMaterialCode("WL-1234")).thenReturn(null);
        when(drawingMapper.selectByDrawingNoAndVersion(any(), any())).thenReturn(null);
        when(drawingMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenAnswer(inv -> {
            ((CrmDrawing) inv.getArgument(0)).setId(1L); return 1;
        });
        when(versionMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingVersion.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        DrawingCreateRequest req = new DrawingCreateRequest();
        req.setTitle("valid material code");
        req.setMaterialCode("WL-1234");
        req.setProcessRoute("[{\"step\":1,\"name\":\"X\",\"cost\":1}]");
        Result<CrmDrawing> r = svc.createDrawing(req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void invalid_wl_format_rejected() {
        DrawingService svc = newSvc();
        DrawingCreateRequest req = new DrawingCreateRequest();
        req.setTitle("invalid format");
        req.setMaterialCode("XX-9999");  // 非 WL 前缀
            req.setProcessRoute("[{\"step\":1,\"name\":\"X\",\"cost\":1}]");
        Result<CrmDrawing> r = svc.createDrawing(req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void duplicate_wl_xxxx_rejected_40905() {
        CrmDrawing existing = new CrmDrawing();
        existing.setId(99L); existing.setMaterialCode("WL-1234");
        when(drawingMapper.selectByMaterialCode("WL-1234")).thenReturn(existing);
        DrawingService svc = newSvc();
        DrawingCreateRequest req = new DrawingCreateRequest();
        req.setTitle("dup");
        req.setMaterialCode("WL-1234");
        req.setProcessRoute("[{\"step\":1,\"name\":\"X\",\"cost\":1}]");
        Result<CrmDrawing> r = svc.createDrawing(req, 1001L);
        assertEquals(40905, r.getCode());
        assertEquals("MATERIAL_CODE_DUPLICATE", r.getMessage());
    }
}
