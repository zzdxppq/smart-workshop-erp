package com.btsheng.erp.production.outsource.service;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceStateHistory;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceStateHistoryMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.22 · OutsourceStateMachineService 单元测试（FR-6-2）
 * 18 测例：7 状态合法转换 + REJECTED + REWORK + 40904 守卫 + 终态不可再开 + 状态历史
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutsourceStateMachineServiceTest {

    @Mock private CrmOutsourceOrderMapper orderMapper;
    @Mock private CrmOutsourceStateHistoryMapper stateHistoryMapper;
    @Mock private ErpDocNoGenerator docNoGenerator;

    private OutsourceStateMachineService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new OutsourceStateMachineService(orderMapper, stateHistoryMapper, docNoGenerator);

        when(orderMapper.selectById(any(Long.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return mockOrder(id);
        });
        when(orderMapper.updateById(any(CrmOutsourceOrder.class))).thenReturn(1);
        when(stateHistoryMapper.insert(any(CrmOutsourceStateHistory.class))).thenAnswer(inv -> {
            CrmOutsourceStateHistory h = inv.getArgument(0);
            h.setId(1L);
            return 1;
        });
    }

    private CrmOutsourceOrder mockOrder(Long id) {
        CrmOutsourceOrder o = new CrmOutsourceOrder();
        o.setId(id);
        o.setOutsourceNo("WW20260612-0001");
        o.setWorkorderNo("GD20260612-0001");
        o.setStatus(OutsourceStateMachineService.STATE_DRAFT);
        o.setReworkCount(0);
        o.setCreatedAt(LocalDateTime.now());
        return o;
    }

    private void mockOrderWithState(Long id, String state) {
        when(orderMapper.selectById(id)).thenAnswer(inv -> {
            Long oid = inv.getArgument(0);
            CrmOutsourceOrder o = new CrmOutsourceOrder();
            o.setId(oid);
            o.setOutsourceNo("WW20260612-0001");
            o.setStatus(state);
            o.setReworkCount(0);
            return o;
        });
    }

    // ====== 7 状态合法转换 7 测例 ======
            @Test
    @DisplayName("7 状态机 DRAFT → SENT")
    void testAdvance_Draft_To_Sent() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_DRAFT);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_SENT, "生管", 2L, "提交");
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_SENT, r.getData().getStatus());
    }

    @Test
    @DisplayName("7 状态机 SENT → ACCEPTED")
    void testAdvance_Sent_To_Accepted() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_SENT);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_ACCEPTED, "采购", 102L, "接单");
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_ACCEPTED, r.getData().getStatus());
        assertNotNull(r.getData().getAcceptedAt());
    }

    @Test
    @DisplayName("7 状态机 ACCEPTED → IN_PRODUCTION")
    void testAdvance_Accepted_To_InProduction() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_ACCEPTED);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_IN_PRODUCTION, "采购", 102L, "开始生产");
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_IN_PRODUCTION, r.getData().getStatus());
    }

    @Test
    @DisplayName("7 状态机 IN_PRODUCTION → INSPECTED")
    void testAdvance_InProduction_To_Inspected() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_IN_PRODUCTION);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_INSPECTED, "品检", 201L, "送检");
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_INSPECTED, r.getData().getStatus());
    }

    @Test
    @DisplayName("7 状态机 INSPECTED → COMPLETED")
    void testAdvance_Inspected_To_Completed() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_INSPECTED);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_COMPLETED, "品检", 201L, "完工");
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_COMPLETED, r.getData().getStatus());
        assertNotNull(r.getData().getCompletedAt());
    }

    @Test
    @DisplayName("7 状态机 COMPLETED → CLOSED")
    void testAdvance_Completed_To_Closed() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_COMPLETED);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_CLOSED, "财务", 301L, "关单");
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_CLOSED, r.getData().getStatus());
        assertNotNull(r.getData().getClosedAt());
    }

    @Test
    @DisplayName("完整 lifecycle 7 步")
    void testAdvance_FullLifecycle() {
        CrmOutsourceOrder o = mockOrder(1L);
        o.setStatus(OutsourceStateMachineService.STATE_DRAFT);
        when(orderMapper.selectById(1L)).thenReturn(o);

        // DRAFT → SENT → ACCEPTED → IN_PRODUCTION → INSPECTED → COMPLETED → CLOSED
            service.advanceState(1L, OutsourceStateMachineService.STATE_SENT, "生管", 2L, null);
        o.setStatus(OutsourceStateMachineService.STATE_SENT);

        service.advanceState(1L, OutsourceStateMachineService.STATE_ACCEPTED, "采购", 102L, null);
        o.setStatus(OutsourceStateMachineService.STATE_ACCEPTED);

        service.advanceState(1L, OutsourceStateMachineService.STATE_IN_PRODUCTION, "采购", 102L, null);
        o.setStatus(OutsourceStateMachineService.STATE_IN_PRODUCTION);

        service.advanceState(1L, OutsourceStateMachineService.STATE_INSPECTED, "品检", 201L, null);
        o.setStatus(OutsourceStateMachineService.STATE_INSPECTED);

        service.advanceState(1L, OutsourceStateMachineService.STATE_COMPLETED, "品检", 201L, null);
        o.setStatus(OutsourceStateMachineService.STATE_COMPLETED);

        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_CLOSED, "财务", 301L, null);
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_CLOSED, r.getData().getStatus());
    }

    // ====== REJECTED 路径 2 测例 ======
            @Test
    @DisplayName("rollback → REJECTED 拒收（任意非终态）")
    void testRollback_To_Rejected() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_INSPECTED);
        Result<CrmOutsourceOrder> r = service.rollbackState(1L, "来料不良", "品检", 201L);
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_REJECTED, r.getData().getStatus());
    }

    @Test
    @DisplayName("rollback 原因必填")
    void testRollback_Reason_Required() {
        Result<CrmOutsourceOrder> r = service.rollbackState(1L, null, "品检", 201L);
        assertEquals(40001, r.getCode());
    }

    // ====== REWORK 返修路径 2 测例 ======
            @Test
    @DisplayName("INSPECTED → REWORK 返修路径")
    void testAdvance_Inspected_To_Rework() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_INSPECTED);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_REWORK, "品检", 201L, "返工");
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_REWORK, r.getData().getStatus());
    }

    @Test
    @DisplayName("REWORK → IN_PRODUCTION 重新生产")
    void testAdvance_Rework_To_InProduction() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_REWORK);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_IN_PRODUCTION, "采购", 102L, "再次生产");
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_IN_PRODUCTION, r.getData().getStatus());
    }

    // ====== 状态守卫 40904 OUTSOURCE_STATE_INVALID 4 测例 ======
            @Test
    @DisplayName("状态守卫 DRAFT 不可直接 IN_PRODUCTION → 40904")
    void testGuard_InvalidTransition_Draft_To_InProduction() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_DRAFT);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_IN_PRODUCTION, "采购", 102L, null);
        assertEquals(40904, r.getCode());
    }

    @Test
    @DisplayName("状态守卫 ACCEPTED 不可回退到 SENT → 40904")
    void testGuard_InvalidTransition_Accepted_To_Sent() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_ACCEPTED);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_SENT, "采购", 102L, null);
        assertEquals(40904, r.getCode());
    }

    @Test
    @DisplayName("状态守卫 INSPECTED 不可直接 CLOSED（需先 COMPLETED）→ 40904")
    void testGuard_InvalidTransition_Inspected_To_Closed() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_INSPECTED);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_CLOSED, "财务", 301L, null);
        assertEquals(40904, r.getCode());
    }

    @Test
    @DisplayName("状态守卫 validateStateTransition 函数")
    void testValidateStateTransition() {
        assertTrue(service.validateStateTransition("DRAFT", "SENT"));
        assertTrue(service.validateStateTransition("COMPLETED", "CLOSED"));
        assertFalse(service.validateStateTransition("DRAFT", "IN_PRODUCTION"));
        assertFalse(service.validateStateTransition("CLOSED", "SENT"));
        assertFalse(service.validateStateTransition(null, "SENT"));
        assertFalse(service.validateStateTransition("SENT", null));
    }

    // ====== 终态 CLOSED / REJECTED 不可再开 2 测例 ======
            @Test
    @DisplayName("终态 CLOSED 不可再 advance → 40904")
    void testTerminal_Closed_Cannot_Advance() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_CLOSED);
        Result<CrmOutsourceOrder> r = service.advanceState(1L, OutsourceStateMachineService.STATE_REWORK, "品检", 201L, "翻工");
        assertEquals(40904, r.getCode());
    }

    @Test
    @DisplayName("终态 REJECTED 不可再 rollback → 40904")
    void testTerminal_Rejected_Cannot_Rollback() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_REJECTED);
        Result<CrmOutsourceOrder> r = service.rollbackState(1L, "再退", "财务", 301L);
        assertEquals(40904, r.getCode());
    }

    // ====== 状态历史 = 1 测例 ======
            @Test
    @DisplayName("状态历史查询")
    void testGetStateHistory() {
        CrmOutsourceStateHistory h1 = new CrmOutsourceStateHistory();
        h1.setId(1L);
        h1.setOutsourceId(1L);
        h1.setFromState("DRAFT");
        h1.setToState("SENT");
        when(stateHistoryMapper.selectByOutsourceId(1L)).thenReturn(List.of(h1));

        Result<List<CrmOutsourceStateHistory>> r = service.getStateHistory(1L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    // ====== 辅助 API 测例 ======
            @Test
    @DisplayName("获取委外单当前状态")
    void testGetOutsourceState() {
        mockOrderWithState(1L, OutsourceStateMachineService.STATE_IN_PRODUCTION);
        Result<CrmOutsourceOrder> r = service.getOutsourceState(1L);
        assertEquals(0, r.getCode());
        assertEquals(OutsourceStateMachineService.STATE_IN_PRODUCTION, r.getData().getStatus());
    }

    @Test
    @DisplayName("获取委外单状态 不存在 → 40404")
    void testGetOutsourceState_NotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);
        Result<CrmOutsourceOrder> r = service.getOutsourceState(999L);
        assertEquals(40404, r.getCode());
    }

    @Test
    @DisplayName("状态转换矩阵 API")
    void testTransitionMatrix() {
        Result<Map<String, Object>> r = service.getTransitionMatrix();
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().get("transitions"));
        assertNotNull(r.getData().get("states"));
    }
}
