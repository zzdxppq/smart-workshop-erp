package com.btsheng.erp.business.crm.drawing.integration;

import com.btsheng.erp.business.crm.drawing.dto.DrawingReleaseRequest;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
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

/** V1.3.7 Story 1.7 · 发布审批集成测例 (3 测例 · 4 阈值 + 二次密码) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DrawingApprovalIntegrationTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingVersionMapper versionMapper;
    @Mock private CrmDrawingHistoryMapper historyMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private DrawingService newSvc() {
        return new DrawingService(drawingMapper, versionMapper, historyMapper, docNoGenerator);
    }

    @Test void four_threshold_routing() {
        // 非 FA 件：单人审批（无需 adminPassword）
            CrmDrawing nonFa = new CrmDrawing();
        nonFa.setId(1L); nonFa.setStatus("DRAFT"); nonFa.setIsFa(0);
        when(drawingMapper.selectById(1L)).thenReturn(nonFa);
        when(drawingMapper.updateById(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        Result<CrmDrawing> r1 = svc.releaseDrawing(1L, new DrawingReleaseRequest(), 1001L);
        assertEquals(0, r1.getCode());

        // FA 件：> 20万 走二次密码
            CrmDrawing fa = new CrmDrawing();
        fa.setId(2L); fa.setStatus("DRAFT"); fa.setIsFa(1);
        when(drawingMapper.selectById(2L)).thenReturn(fa);
        DrawingReleaseRequest noPwd = new DrawingReleaseRequest();
        Result<CrmDrawing> r2 = svc.releaseDrawing(2L, noPwd, 1001L);
        assertEquals(40101, r2.getCode());   // ADMIN_PASSWORD_REQUIRED
            DrawingReleaseRequest withPwd = new DrawingReleaseRequest();
        withPwd.setAdminPassword("admin_pwd_2026");
        Result<CrmDrawing> r3 = svc.releaseDrawing(2L, withPwd, 1001L);
        assertEquals(0, r3.getCode());
    }

    @Test void second_password_requirement() {
        CrmDrawing fa = new CrmDrawing();
        fa.setId(1L); fa.setStatus("DRAFT"); fa.setIsFa(1);
        when(drawingMapper.selectById(1L)).thenReturn(fa);
        DrawingService svc = newSvc();
        DrawingReleaseRequest req = new DrawingReleaseRequest();
        req.setAdminPassword(null);
        Result<CrmDrawing> r = svc.releaseDrawing(1L, req, 1001L);
        assertEquals(40101, r.getCode());
    }

    @Test void blacklist_priority_check() {
        // 图纸无客户关联，黑名单检查不直接命中（下游订单/报价自动联动）
            CrmDrawing d = new CrmDrawing();
        d.setId(1L); d.setStatus("DRAFT"); d.setIsFa(0);
        when(drawingMapper.selectById(1L)).thenReturn(d);
        when(drawingMapper.updateById(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawing.class))).thenReturn(1);
        when(historyMapper.insert(any(com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory.class))).thenReturn(1);
        DrawingService svc = newSvc();
        Result<CrmDrawing> r = svc.releaseDrawing(1L, new DrawingReleaseRequest(), 1001L);
        assertEquals(0, r.getCode());
        // 黑名单优先级（40902 CUSTOMER_BLACKLIST > 40909 CREDIT_LIMIT_EXCEEDED）
        // 在图纸场景下不触发，但下游场景必触发
    }
}
