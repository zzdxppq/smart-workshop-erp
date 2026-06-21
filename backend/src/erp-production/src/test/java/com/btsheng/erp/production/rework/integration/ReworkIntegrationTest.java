package com.btsheng.erp.production.rework.integration;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.production.rework.entity.CrmRework;
import com.btsheng.erp.production.rework.entity.CrmReworkAlert;
import com.btsheng.erp.production.rework.entity.CrmReworkHistory;
import com.btsheng.erp.production.rework.mapper.CrmReworkAlertMapper;
import com.btsheng.erp.production.rework.mapper.CrmReworkHistoryMapper;
import com.btsheng.erp.production.rework.mapper.CrmReworkMapper;
import com.btsheng.erp.production.rework.service.ReworkService;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.23 · ReworkService 集成测试（FR-6-3）
 * 10 测例：完整 lifecycle + 次数限制 + 成本计入 + 预警触发 + 跨模块
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReworkIntegrationTest {

    @Mock private CrmReworkMapper reworkMapper;
    @Mock private CrmReworkHistoryMapper historyMapper;
    @Mock private CrmReworkAlertMapper alertMapper;
    @Mock private CrmOutsourceOrderMapper orderMapper;
    @Mock private ErpDocNoGenerator docNoGenerator;

    private ReworkService service;

    @BeforeEach
    void setUp() {
        service = new ReworkService(reworkMapper, historyMapper, alertMapper, orderMapper, docNoGenerator);
        when(docNoGenerator.nextReworkNo()).thenReturn("RW20260612-0001", "RW20260612-0002", "RW20260612-0003");
        when(reworkMapper.insert(any(CrmRework.class))).thenAnswer(inv -> {
            CrmRework r = inv.getArgument(0);
            r.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(historyMapper.insert(any(CrmReworkHistory.class))).thenAnswer(inv -> {
            CrmReworkHistory h = inv.getArgument(0);
            h.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(alertMapper.insert(any(CrmReworkAlert.class))).thenAnswer(inv -> {
            CrmReworkAlert a = inv.getArgument(0);
            a.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(orderMapper.updateById(any(CrmOutsourceOrder.class))).thenReturn(1);
    }

    private CrmOutsourceOrder mockOrder(Long id, int reworkCount) {
        CrmOutsourceOrder o = new CrmOutsourceOrder();
        o.setId(id);
        o.setOutsourceNo("WW20260612-0001");
        o.setStatus("INSPECTED");
        o.setReworkCount(reworkCount);
        return o;
    }

    // ====== 完整 lifecycle = 2 测例 ======
            @Test
    @DisplayName("集成 lifecycle 1：3 次返修完整闭环")
    void testIntegration_FullLifecycle_3Reworks() {
        // 第一次返修
            when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 0));
        Result<CrmRework> r1 = service.createRework(1L, "首次返修", new BigDecimal("100"), 201L);
        assertEquals(0, r1.getCode());
        assertEquals(1, r1.getData().getReworkCount());

        // 完成第一次
            CrmRework rework1 = new CrmRework();
        rework1.setId(r1.getData().getId());
        rework1.setStatus("IN_PROGRESS");
        when(reworkMapper.selectById(r1.getData().getId())).thenReturn(rework1);
        Result<CrmRework> f1 = service.finishRework(r1.getData().getId(), 201L);
        assertEquals(0, f1.getCode());
        assertEquals("COMPLETED", f1.getData().getStatus());

        // 第二次返修
            when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 1));
        Result<CrmRework> r2 = service.createRework(1L, "第二次返修", new BigDecimal("150"), 201L);
        assertEquals(0, r2.getCode());
        assertEquals(2, r2.getData().getReworkCount());

        // 第三次返修
            when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 2));
        Result<CrmRework> r3 = service.createRework(1L, "第三次返修", new BigDecimal("200"), 201L);
        assertEquals(0, r3.getCode());
        assertEquals(3, r3.getData().getReworkCount());
    }

    @Test
    @DisplayName("集成 lifecycle 2：达到上限 3 后第 4 次被拒绝")
    void testIntegration_FullLifecycle_ReachLimit() {
        // 第三次返修成功
            when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 2));
        Result<CrmRework> r3 = service.createRework(1L, "第三次", new BigDecimal("200"), 201L);
        assertEquals(0, r3.getCode());

        // 第 4 次被拒
            when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 3));
        Result<CrmRework> r4 = service.createRework(1L, "第四次", new BigDecimal("200"), 201L);
        assertEquals(40905, r4.getCode());
    }

    // ====== 返修次数限制 跨服务 = 2 测例 ======
            @Test
    @DisplayName("返修次数限制：3 次后阻断 跨服务调用")
    void testIntegration_LimitBlock() {
        when(orderMapper.selectById(2L)).thenReturn(mockOrder(2L, 3));

        Result<CrmRework> r = service.createRework(2L, "blocked", new BigDecimal("100"), 201L);
        assertEquals(40905, r.getCode());

        // 阻止了 insert
            verify(reworkMapper, never()).insert(any(CrmRework.class));
    }

    @Test
    @DisplayName("返修次数限制：刚到 3 还能返修 1 次")
    void testIntegration_LimitEdge() {
        when(orderMapper.selectById(3L)).thenReturn(mockOrder(3L, 2));

        Result<CrmRework> r = service.createRework(3L, "edge", new BigDecimal("100"), 201L);
        assertEquals(0, r.getCode());
        assertEquals(3, r.getData().getReworkCount());
    }

    // ====== 返修成本计入 = 2 测例 ======
            @Test
    @DisplayName("返修成本累计 跨多次返修")
    void testIntegration_CostAccumulate() {
        when(orderMapper.selectById(4L)).thenReturn(mockOrder(4L, 0));
        service.createRework(4L, "1st", new BigDecimal("100"), 201L);
        when(orderMapper.selectById(4L)).thenReturn(mockOrder(4L, 1));
        service.createRework(4L, "2nd", new BigDecimal("150"), 201L);

        // 累加 = 250
            CrmRework r1 = new CrmRework();
        r1.setCost(new BigDecimal("100"));
        CrmRework r2 = new CrmRework();
        r2.setCost(new BigDecimal("150"));
        when(reworkMapper.selectByOutsourceId(4L)).thenReturn(List.of(r1, r2));

        Result<BigDecimal> total = service.getTotalReworkCost(4L);
        assertEquals(0, total.getCode());
        assertEquals(0, total.getData().compareTo(new BigDecimal("250")));
    }

    @Test
    @DisplayName("返修成本 0 允许")
    void testIntegration_CostZero() {
        when(orderMapper.selectById(5L)).thenReturn(mockOrder(5L, 0));
        Result<CrmRework> r = service.createRework(5L, "free rework", BigDecimal.ZERO, 201L);
        assertEquals(0, r.getCode());
    }

    // ====== 返修次数预警触发 = 2 测例 ======
            @Test
    @DisplayName("预警触发：第 2 次返修 → WARN 级别")
    void testIntegration_AlertTrigger_Warn() {
        when(orderMapper.selectById(6L)).thenReturn(mockOrder(6L, 1));
        when(alertMapper.selectByOutsourceId(6L)).thenReturn(new ArrayList<>());

        service.createRework(6L, "2nd", new BigDecimal("100"), 201L);

        verify(alertMapper, atLeastOnce()).insert(argThat((CrmReworkAlert a) -> "WARN".equals(a.getAlertLevel())));
    }

    @Test
    @DisplayName("预警触发：第 3 次返修 → CRITICAL 级别")
    void testIntegration_AlertTrigger_Critical() {
        when(orderMapper.selectById(7L)).thenReturn(mockOrder(7L, 2));
        when(alertMapper.selectByOutsourceId(7L)).thenReturn(new ArrayList<>());

        service.createRework(7L, "3rd", new BigDecimal("100"), 201L);

        verify(alertMapper, atLeastOnce()).insert(argThat((CrmReworkAlert a) -> "CRITICAL".equals(a.getAlertLevel())));
    }

    // ====== 跨模块 1.18 委外单联动 = 2 测例 ======
            @Test
    @DisplayName("跨模块 1.18 委外单：返修次数同步累加")
    void testIntegration_Cross_18_ReworkCountSync() {
        when(orderMapper.selectById(8L)).thenReturn(mockOrder(8L, 0));

        service.createRework(8L, "sync", new BigDecimal("100"), 201L);

        verify(orderMapper, atLeastOnce()).updateById(argThat((CrmOutsourceOrder o) -> o.getReworkCount() != null && o.getReworkCount() == 1));
    }

    @Test
    @DisplayName("跨模块 1.18 委外单：委外单不存在 → 40404")
    void testIntegration_Cross_18_OrderNotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);

        Result<CrmRework> r = service.createRework(999L, "not found", new BigDecimal("100"), 201L);
        assertEquals(40404, r.getCode());
    }
}
