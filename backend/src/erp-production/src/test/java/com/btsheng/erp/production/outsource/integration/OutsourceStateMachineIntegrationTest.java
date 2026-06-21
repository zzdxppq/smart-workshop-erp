package com.btsheng.erp.production.outsource.integration;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceStateHistory;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceStateHistoryMapper;
import com.btsheng.erp.production.outsource.service.OutsourceService;
import com.btsheng.erp.production.outsource.service.OutsourceStateMachineService;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.22 · 委外 7 状态机集成测试（FR-6-2）
 * 12 测例：完整 lifecycle + 守卫跨服务 + 审计留痕 + 跨模块 + 状态历史
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutsourceStateMachineIntegrationTest {

    @Mock private CrmOutsourceOrderMapper orderMapper;
    @Mock private CrmOutsourceStateHistoryMapper stateHistoryMapper;
    @Mock private ErpDocNoGenerator docNoGenerator;

    private OutsourceStateMachineService stateMachineService;

    private void setUpStateMachine() {
        stateMachineService = new OutsourceStateMachineService(orderMapper, stateHistoryMapper, docNoGenerator);
        when(orderMapper.updateById(any(CrmOutsourceOrder.class))).thenReturn(1);
        when(stateHistoryMapper.insert(any(CrmOutsourceStateHistory.class))).thenAnswer(inv -> {
            CrmOutsourceStateHistory h = inv.getArgument(0);
            h.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
    }

    private CrmOutsourceOrder mockOrder(Long id, String state) {
        CrmOutsourceOrder o = new CrmOutsourceOrder();
        o.setId(id);
        o.setOutsourceNo("WW20260612-0001");
        o.setWorkorderNo("GD20260612-0001");
        o.setStatus(state);
        o.setReworkCount(0);
        o.setCreatedAt(LocalDateTime.now());
        return o;
    }

    // ====== 完整 lifecycle = 2 测例 ======
            @Test
    @DisplayName("集成 lifecycle 1：DRAFT → CLOSED 7 步")
    void testIntegration_FullLifecycle() {
        setUpStateMachine();
        CrmOutsourceOrder o = mockOrder(1L, OutsourceStateMachineService.STATE_DRAFT);
        when(orderMapper.selectById(1L)).thenReturn(o);

        Result<CrmOutsourceOrder> r1 = stateMachineService.advanceState(1L, OutsourceStateMachineService.STATE_SENT, "生管", 2L, "提交");
        o.setStatus(r1.getData().getStatus());
        assertEquals(0, r1.getCode());

        Result<CrmOutsourceOrder> r2 = stateMachineService.advanceState(1L, OutsourceStateMachineService.STATE_ACCEPTED, "采购", 102L, "接单");
        o.setStatus(r2.getData().getStatus());
        assertEquals(0, r2.getCode());

        Result<CrmOutsourceOrder> r3 = stateMachineService.advanceState(1L, OutsourceStateMachineService.STATE_IN_PRODUCTION, "采购", 102L, "生产");
        o.setStatus(r3.getData().getStatus());
        assertEquals(0, r3.getCode());

        Result<CrmOutsourceOrder> r4 = stateMachineService.advanceState(1L, OutsourceStateMachineService.STATE_INSPECTED, "品检", 201L, "送检");
        o.setStatus(r4.getData().getStatus());
        assertEquals(0, r4.getCode());

        Result<CrmOutsourceOrder> r5 = stateMachineService.advanceState(1L, OutsourceStateMachineService.STATE_COMPLETED, "品检", 201L, "完工");
        o.setStatus(r5.getData().getStatus());
        assertEquals(0, r5.getCode());

        Result<CrmOutsourceOrder> r6 = stateMachineService.advanceState(1L, OutsourceStateMachineService.STATE_CLOSED, "财务", 301L, "关单");
        o.setStatus(r6.getData().getStatus());
        assertEquals(0, r6.getCode());
        assertEquals(OutsourceStateMachineService.STATE_CLOSED, r6.getData().getStatus());

        // 7 步留痕
            verify(stateHistoryMapper, times(6)).insert(any(CrmOutsourceStateHistory.class));
    }

    @Test
    @DisplayName("集成 lifecycle 2：返修闭环 多次返修 → INSPECTED → COMPLETED → CLOSED")
    void testIntegration_ReworkLoop() {
        setUpStateMachine();
        CrmOutsourceOrder o = mockOrder(2L, OutsourceStateMachineService.STATE_INSPECTED);
        when(orderMapper.selectById(2L)).thenReturn(o);

        // INSPECTED → REWORK
            Result<CrmOutsourceOrder> r1 = stateMachineService.advanceState(2L, OutsourceStateMachineService.STATE_REWORK, "品检", 201L, "返工");
        o.setStatus(r1.getData().getStatus());
        o.setReworkCount(1);
        assertEquals(0, r1.getCode());

        // REWORK → IN_PRODUCTION
            Result<CrmOutsourceOrder> r2 = stateMachineService.advanceState(2L, OutsourceStateMachineService.STATE_IN_PRODUCTION, "采购", 102L, "再次生产");
        o.setStatus(r2.getData().getStatus());
        assertEquals(0, r2.getCode());

        // IN_PRODUCTION → INSPECTED
            Result<CrmOutsourceOrder> r3 = stateMachineService.advanceState(2L, OutsourceStateMachineService.STATE_INSPECTED, "品检", 201L, "再次送检");
        o.setStatus(r3.getData().getStatus());
        assertEquals(0, r3.getCode());

        // INSPECTED → COMPLETED
            Result<CrmOutsourceOrder> r4 = stateMachineService.advanceState(2L, OutsourceStateMachineService.STATE_COMPLETED, "品检", 201L, "完工");
        o.setStatus(r4.getData().getStatus());
        assertEquals(0, r4.getCode());

        // COMPLETED → CLOSED
            Result<CrmOutsourceOrder> r5 = stateMachineService.advanceState(2L, OutsourceStateMachineService.STATE_CLOSED, "财务", 301L, "关单");
        assertEquals(0, r5.getCode());
        assertEquals(OutsourceStateMachineService.STATE_CLOSED, r5.getData().getStatus());
    }

    // ====== 状态守卫跨服务 = 3 测例 ======
            @Test
    @DisplayName("状态守卫：跨服务非法转换 DRAFT → COMPLETED")
    void testIntegration_Guards_DraftToCompleted() {
        setUpStateMachine();
        CrmOutsourceOrder o = mockOrder(3L, OutsourceStateMachineService.STATE_DRAFT);
        when(orderMapper.selectById(3L)).thenReturn(o);

        Result<CrmOutsourceOrder> r = stateMachineService.advanceState(3L, OutsourceStateMachineService.STATE_COMPLETED, "品检", 201L, "跳过");
        assertEquals(40904, r.getCode());
    }

    @Test
    @DisplayName("状态守卫：跨服务 非法 ACCEPTED → REWORK（不可逆）")
    void testIntegration_Guards_AcceptedToRework() {
        setUpStateMachine();
        CrmOutsourceOrder o = mockOrder(4L, OutsourceStateMachineService.STATE_ACCEPTED);
        when(orderMapper.selectById(4L)).thenReturn(o);

        Result<CrmOutsourceOrder> r = stateMachineService.advanceState(4L, OutsourceStateMachineService.STATE_REWORK, "品检", 201L, "跳返工");
        assertEquals(40904, r.getCode());
    }

    @Test
    @DisplayName("状态守卫：跨服务 非法 SENT → CLOSED")
    void testIntegration_Guards_SentToClosed() {
        setUpStateMachine();
        CrmOutsourceOrder o = mockOrder(5L, OutsourceStateMachineService.STATE_SENT);
        when(orderMapper.selectById(5L)).thenReturn(o);

        Result<CrmOutsourceOrder> r = stateMachineService.advanceState(5L, OutsourceStateMachineService.STATE_CLOSED, "财务", 301L, "跳关");
        assertEquals(40904, r.getCode());
    }

    // ====== 审计留痕 = 2 测例 ======
            @Test
    @DisplayName("审计留痕：每次状态转换写 1 条 history")
    void testIntegration_AuditTrail() {
        setUpStateMachine();
        CrmOutsourceOrder o = mockOrder(6L, OutsourceStateMachineService.STATE_DRAFT);
        when(orderMapper.selectById(6L)).thenReturn(o);

        stateMachineService.advanceState(6L, OutsourceStateMachineService.STATE_SENT, "生管", 2L, "step1");
        o.setStatus(OutsourceStateMachineService.STATE_SENT);
        stateMachineService.advanceState(6L, OutsourceStateMachineService.STATE_ACCEPTED, "采购", 102L, "step2");
        o.setStatus(OutsourceStateMachineService.STATE_ACCEPTED);
        stateMachineService.rollbackState(6L, "拒收", "采购", 102L);

        verify(stateHistoryMapper, times(3)).insert(any(CrmOutsourceStateHistory.class));
    }

    @Test
    @DisplayName("审计留痕：rollback 写入 ROLLBACK transition_type")
    void testIntegration_AuditTrail_Rollback() {
        setUpStateMachine();
        CrmOutsourceOrder o = mockOrder(7L, OutsourceStateMachineService.STATE_INSPECTED);
        when(orderMapper.selectById(7L)).thenReturn(o);

        stateMachineService.rollbackState(7L, "来料不良", "品检", 201L);

        verify(stateHistoryMapper, times(1)).insert(argThat((CrmOutsourceStateHistory h) ->
            "ROLLBACK".equals(h.getTransitionType())
                && "INSPECTED".equals(h.getFromState())
                && "REJECTED".equals(h.getToState())
        ));
    }

    // ====== 跨模块 1.18 联动 = 3 测例 ======
            @Test
    @DisplayName("跨模块 1.18 → 1.22：委外下单（DRAFT）→ 状态机推进")
    void testIntegration_Cross_18_22_Draft() {
        setUpStateMachine();
        // 1.18 委外下单默认 DRAFT
            CrmOutsourceOrder o = mockOrder(8L, OutsourceStateMachineService.STATE_DRAFT);
        o.setOutsourceNo("WW20260612-0008");
        o.setWorkorderNo("GD20260612-0008");
        when(orderMapper.selectById(8L)).thenReturn(o);

        // 1.22 状态机从 DRAFT 推进
            Result<CrmOutsourceOrder> r = stateMachineService.advanceState(8L, OutsourceStateMachineService.STATE_SENT, "生管", 2L, "提交供应商");
        assertEquals(0, r.getCode());
        assertEquals("WW20260612-0008", r.getData().getOutsourceNo());
    }

    @Test
    @DisplayName("跨模块 1.18 → 1.22：委外 COMPLETED 状态由状态机进入")
    void testIntegration_Cross_18_22_Completed() {
        setUpStateMachine();
        CrmOutsourceOrder o = mockOrder(9L, OutsourceStateMachineService.STATE_INSPECTED);
        o.setOutsourceNo("WW20260612-0009");
        when(orderMapper.selectById(9L)).thenReturn(o);

        Result<CrmOutsourceOrder> r = stateMachineService.advanceState(9L, OutsourceStateMachineService.STATE_COMPLETED, "品检", 201L, "完工");
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_COMPLETED, r.getData().getStatus());
        assertNotNull(r.getData().getCompletedAt());
    }

    @Test
    @DisplayName("跨模块 1.18 → 1.22：委外状态机拒绝非法角色操作（AD-1）")
    void testIntegration_Cross_18_22_RoleCheck() {
        setUpStateMachine();
        CrmOutsourceOrder o = mockOrder(10L, OutsourceStateMachineService.STATE_SENT);
        when(orderMapper.selectById(10L)).thenReturn(o);

        // 推进到 ACCEPTED：需要 采购/品检/admin 角色
        // 生管 角色无权限推进到 ACCEPTED
            Result<CrmOutsourceOrder> r = stateMachineService.advanceState(10L, OutsourceStateMachineService.STATE_ACCEPTED, "生管", 2L, "生管想接单");
        assertEquals(40301, r.getCode());
    }

    // ====== 状态历史查询 = 2 测例 ======
            @Test
    @DisplayName("状态历史查询：按 outsourceId 返回时间线")
    void testIntegration_History_Timeline() {
        setUpStateMachine();
        CrmOutsourceStateHistory h1 = new CrmOutsourceStateHistory();
        h1.setId(1L);
        h1.setOutsourceId(11L);
        h1.setFromState("DRAFT");
        h1.setToState("SENT");
        h1.setOccurredAt(LocalDateTime.now());

        CrmOutsourceStateHistory h2 = new CrmOutsourceStateHistory();
        h2.setId(2L);
        h2.setOutsourceId(11L);
        h2.setFromState("SENT");
        h2.setToState("ACCEPTED");
        h2.setOccurredAt(LocalDateTime.now());

        when(stateHistoryMapper.selectByOutsourceId(11L)).thenReturn(List.of(h1, h2));

        Result<List<CrmOutsourceStateHistory>> r = stateMachineService.getStateHistory(11L);
        assertEquals(0, r.getCode());
        assertEquals(2, r.getData().size());
        assertEquals("SENT", r.getData().get(0).getToState());
        assertEquals("ACCEPTED", r.getData().get(1).getToState());
    }

    @Test
    @DisplayName("状态历史查询：空历史")
    void testIntegration_History_Empty() {
        setUpStateMachine();
        when(stateHistoryMapper.selectByOutsourceId(99L)).thenReturn(new ArrayList<>());

        Result<List<CrmOutsourceStateHistory>> r = stateMachineService.getStateHistory(99L);
        assertEquals(0, r.getCode());
        assertEquals(0, r.getData().size());
    }
}
