package com.btsheng.erp.business.crm.workflowevent;

import com.btsheng.erp.business.crm.workflowevent.dto.WorkflowEventStatsDTO;
import com.btsheng.erp.business.crm.workflowevent.entity.SysWorkflowEvent;
import com.btsheng.erp.business.crm.workflowevent.mapper.SysWorkflowEventMapper;
import com.btsheng.erp.business.crm.workflowevent.service.WorkflowEventService;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.8 Sprint 8 Story 8.3 · WorkflowEventService 单元测例
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */
@DisplayName("Story 8.3 · WorkflowEventService 单元测例（V1.3.8 Sprint 8）")
class WorkflowEventServiceTest {

    private SysWorkflowEventMapper mapper;
    private WorkflowEventService service;

    @BeforeEach
    void setup() {
        mapper = mock(SysWorkflowEventMapper.class);
        when(mapper.insert(any(SysWorkflowEvent.class))).thenReturn(1);
        service = new WorkflowEventService(mapper);
    }

    // ===== AC-8.3.1 事件记录（6 测例） =====
            @Test
    @DisplayName("AC-8.3.1.a APPROVED 事件记录成功")
    void record_approved_event() {
        Result<SysWorkflowEvent> r = service.recordEvent(
                "PO_APPROVAL", 8008L, "PO-20260613-0008",
                WorkflowEventService.EVENT_APPROVED, "PROCUREMENT_MANAGER",
                1001L, "采购主管A", "审批通过", 5, "AMOUNT_10K_50K");

        assertTrue(r.isSuccess() || r.getCode() == 0);
        assertNotNull(r.getData());
        assertTrue(r.getData().getEventNo().startsWith("EV-"));
    }

    @Test
    @DisplayName("AC-8.3.1.b REJECTED 事件记录")
    void record_rejected_event() {
        Result<SysWorkflowEvent> r = service.recordEvent(
                "PO_APPROVAL", 8009L, "PO-20260613-0009",
                WorkflowEventService.EVENT_REJECTED, "PROCUREMENT_MANAGER",
                1001L, "采购主管A", "金额超限", 6, "AMOUNT_ABOVE_50K");

        assertTrue(r.isSuccess() || r.getCode() == 0);
        assertEquals("REJECTED", r.getData().getEventType());
    }

    @Test
    @DisplayName("AC-8.3.1.c CREATED 事件")
    void record_created_event() {
        Result<SysWorkflowEvent> r = service.recordEvent(
                "PO_APPROVAL", 8010L, "PO-20260613-0010",
                WorkflowEventService.EVENT_CREATED, "PURCHASER",
                null, null, "提交审批", null, null);

        assertTrue(r.isSuccess() || r.getCode() == 0);
        assertEquals("CREATED", r.getData().getEventType());
    }

    @Test
    @DisplayName("AC-8.3.1.d DELEGATED 事件")
    void record_delegated_event() {
        Result<SysWorkflowEvent> r = service.recordEvent(
                "QUOTE_APPROVAL", 5001L, "BJ-20260613-0001",
                WorkflowEventService.EVENT_DELEGATED, "DEPT_MANAGER",
                2001L, "部门经理B", "委托给总经理", 3, null);

        assertTrue(r.isSuccess() || r.getCode() == 0);
        assertEquals("DELEGATED", r.getData().getEventType());
    }

    @Test
    @DisplayName("AC-8.3.1.e event_no 格式 EV-{timestamp}-{uuid}")
    void event_no_format() {
        Result<SysWorkflowEvent> r = service.recordEvent(
                "PO_APPROVAL", 8011L, "PO-20260613-0011",
                WorkflowEventService.EVENT_APPROVED, "GM",
                9999L, "总经理C", "终审", 4, "AMOUNT_ABOVE_50K");

        String eventNo = r.getData().getEventNo();
        assertTrue(eventNo.matches("^EV-\\d{14}-[A-Z0-9]{4}$"), "eventNo 格式错误：" + eventNo);
    }

    @Test
    @DisplayName("AC-8.3.1.f GM 双签事件（PROCUREMENT_MANAGER + GM）")
    void record_gm_dual_sign() {
        Result<SysWorkflowEvent> r = service.recordEvent(
                "PO_APPROVAL", 8012L, "PO-20260613-0012",
                WorkflowEventService.EVENT_APPROVED, "GM+PROCUREMENT_MANAGER",
                9999L, "总经理C + 采购A", "双签", 6, "AMOUNT_ABOVE_50K");

        assertTrue(r.isSuccess() || r.getCode() == 0);
        assertEquals("GM+PROCUREMENT_MANAGER", r.getData().getApproverRole());
    }

    // ===== AC-8.3.1 字段持久化（4 测例） =====
            @Test
    @DisplayName("AC-8.3.1.g 字段全部持久化到 mapper.insert")
    void all_fields_persisted() {
        service.recordEvent(
                "PO_APPROVAL", 8013L, "PO-20260613-0013",
                WorkflowEventService.EVENT_APPROVED, "PROCUREMENT_MANAGER",
                1001L, "采购A", "审核 OK", 5, "AMOUNT_10K_50K");

        ArgumentCaptor<SysWorkflowEvent> captor = ArgumentCaptor.forClass(SysWorkflowEvent.class);
        verify(mapper).insert(captor.capture());
        SysWorkflowEvent saved = captor.getValue();

        assertEquals("PO_APPROVAL", saved.getWorkflowCode());
        assertEquals(8013L, saved.getBizId());
        assertEquals("PO-20260613-0013", saved.getBizNo());
        assertEquals("APPROVED", saved.getEventType());
        assertEquals("PROCUREMENT_MANAGER", saved.getApproverRole());
        assertEquals(1001L, saved.getApproverUserId());
        assertEquals("采购A", saved.getApproverUserName());
        assertEquals("审核 OK", saved.getComment());
        assertEquals(5, saved.getMatchedNodeIndex());
        assertEquals("AMOUNT_10K_50K", saved.getMatchedThreshold());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("AC-8.3.1.h matched 字段可空")
    void matched_fields_nullable() {
        Result<SysWorkflowEvent> r = service.recordEvent(
                "PO_APPROVAL", 8014L, "PO-20260613-0014",
                WorkflowEventService.EVENT_CREATED, "PURCHASER",
                null, null, "创建", null, null);

        assertTrue(r.isSuccess() || r.getCode() == 0);
        assertNull(r.getData().getMatchedNodeIndex());
        assertNull(r.getData().getMatchedThreshold());
        assertNull(r.getData().getApproverUserId());
        assertNull(r.getData().getApproverUserName());
    }

    @Test
    @DisplayName("AC-8.3.1.i event_no 唯一性（UUID 4 位）")
    void event_no_unique() {
        Result<SysWorkflowEvent> r1 = service.recordEvent(
                "PO_APPROVAL", 1L, "PO-1",
                WorkflowEventService.EVENT_CREATED, "PURCHASER", null, null, null, null, null);
        Result<SysWorkflowEvent> r2 = service.recordEvent(
                "PO_APPROVAL", 2L, "PO-2",
                WorkflowEventService.EVENT_CREATED, "PURCHASER", null, null, null, null, null);

        // 不同时间戳 + UUID 应不同（实际可能撞，但概率极低，1ms 内并发会撞）
            assertNotEquals(r1.getData().getEventNo(), r2.getData().getEventNo(),
                "事件号必须唯一（DB 唯一索引兜底）");
    }

    @Test
    @DisplayName("AC-8.3.1.j created_at 必填")
    void created_at_required() {
        Result<SysWorkflowEvent> r = service.recordEvent(
                "PO_APPROVAL", 8015L, "PO-20260613-0015",
                WorkflowEventService.EVENT_APPROVED, "PROCUREMENT_MANAGER",
                1001L, "采购A", "OK", 5, "AMOUNT_10K_50K");

        assertNotNull(r.getData().getCreatedAt());
    }

    // ==================== Sprint 10 Story 10.3 · AC-10.3.1/2 统计聚合（8 测例）====================
            @Test
    @DisplayName("AC-10.3.1.a 按 workflow_code 聚合返回 totalCount")
    void stats_total_count() {
        when(mapper.countByWorkflowCode(eq("PO_APPROVAL"), isNull(), any(), any()))
                .thenReturn(23L);

        Result<WorkflowEventStatsDTO> r = service.stats("PO_APPROVAL", null, null, null);

        assertTrue(r.isSuccess() || r.getCode() == 0);
        assertEquals(23L, r.getData().getTotalCount());
    }

    @Test
    @DisplayName("AC-10.3.1.b byEventType 分组计数")
    void stats_by_event_type() {
        when(mapper.countByWorkflowCode(eq("PO_APPROVAL"), any(), any(), any())).thenReturn(15L);
        when(mapper.aggregateByEventType(eq("PO_APPROVAL"), any(), any(), any()))
                .thenReturn(java.util.List.of(
                        java.util.Map.of("eventType", "CREATED", "cnt", 8L),
                        java.util.Map.of("eventType", "APPROVED", "cnt", 5L),
                        java.util.Map.of("eventType", "REJECTED", "cnt", 2L)));

        Result<WorkflowEventStatsDTO> r = service.stats("PO_APPROVAL", null, null, null);

        assertEquals(8L, r.getData().getByEventType().get("CREATED"));
        assertEquals(5L, r.getData().getByEventType().get("APPROVED"));
        assertEquals(2L, r.getData().getByEventType().get("REJECTED"));
    }

    @Test
    @DisplayName("AC-10.3.1.c byApproverRole 分组计数")
    void stats_by_approver_role() {
        when(mapper.countByWorkflowCode(eq("PO_APPROVAL"), any(), any(), any())).thenReturn(20L);
        when(mapper.aggregateByApproverRole(eq("PO_APPROVAL"), any(), any(), any()))
                .thenReturn(java.util.List.of(
                        java.util.Map.of("approverRole", "PROCUREMENT_MANAGER", "cnt", 12L),
                        java.util.Map.of("approverRole", "GM", "cnt", 5L),
                        java.util.Map.of("approverRole", "GM+PROCUREMENT_MANAGER", "cnt", 3L)));

        Result<WorkflowEventStatsDTO> r = service.stats("PO_APPROVAL", null, null, null);

        assertEquals(12L, r.getData().getByApproverRole().get("PROCUREMENT_MANAGER"));
        assertEquals(5L, r.getData().getByApproverRole().get("GM"));
        assertEquals(3L, r.getData().getByApproverRole().get("GM+PROCUREMENT_MANAGER"));
    }

    @Test
    @DisplayName("AC-10.3.2.a approverRole 过滤生效")
    void stats_approver_role_filter() {
        when(mapper.countByWorkflowCode(eq("PO_APPROVAL"), eq("PROCUREMENT_MANAGER"), any(), any()))
                .thenReturn(15L);

        Result<WorkflowEventStatsDTO> r = service.stats("PO_APPROVAL", "PROCUREMENT_MANAGER", null, null);

        verify(mapper).countByWorkflowCode(eq("PO_APPROVAL"), eq("PROCUREMENT_MANAGER"), any(), any());
        assertEquals(15L, r.getData().getTotalCount());
    }

    @Test
    @DisplayName("AC-10.3.2.b 时间范围默认 30 天前 ~ 今天")
    void stats_default_30_days() {
        when(mapper.countByWorkflowCode(eq("PO_APPROVAL"), any(), any(), any())).thenReturn(0L);

        Result<WorkflowEventStatsDTO> r = service.stats("PO_APPROVAL", null, null, null);

        // 默认 startDate 应是 30 天前，endDate 应是今天
            assertNotNull(r.getData().getPeriod().getStartDate());
        assertNotNull(r.getData().getPeriod().getEndDate());
        assertEquals(30, java.time.temporal.ChronoUnit.DAYS.between(
                r.getData().getPeriod().getStartDate(), r.getData().getPeriod().getEndDate()));
    }

    @Test
    @DisplayName("AC-10.3.2.c 自定义时间范围覆盖默认")
    void stats_custom_period() {
        when(mapper.countByWorkflowCode(eq("PO_APPROVAL"), any(), any(), any())).thenReturn(5L);

        java.time.LocalDate customStart = java.time.LocalDate.of(2026, 1, 1);
        java.time.LocalDate customEnd = java.time.LocalDate.of(2026, 6, 1);

        Result<WorkflowEventStatsDTO> r = service.stats("PO_APPROVAL", null, customStart, customEnd);

        assertEquals(customStart, r.getData().getPeriod().getStartDate());
        assertEquals(customEnd, r.getData().getPeriod().getEndDate());
    }

    @Test
    @DisplayName("AC-10.3.2.d workflowCode 必填校验")
    void stats_workflow_code_required() {
        Result<WorkflowEventStatsDTO> r = service.stats("", null, null, null);
        assertFalse(r.isSuccess() || r.getCode() == 0);
    }

    @Test
    @DisplayName("AC-10.3.2.e null workflowCode 拒绝")
    void stats_null_workflow_code() {
        Result<WorkflowEventStatsDTO> r = service.stats(null, null, null, null);
        assertFalse(r.isSuccess() || r.getCode() == 0);
    }

    @Test
    @DisplayName("TC-10.3.3.3 start_date > end_date 边界（IMPL 约束 5）")
    void stats_start_after_end_rejected() {
        java.time.LocalDate start = java.time.LocalDate.of(2026, 6, 13);
        java.time.LocalDate end = java.time.LocalDate.of(2026, 5, 1);
        Result<WorkflowEventStatsDTO> r = service.stats("PO_APPROVAL", null, start, end);
        assertFalse(r.isSuccess() || r.getCode() == 0);
        assertEquals(40001, r.getCode());
        // 验证 mapper 未被调用（前置校验拦截）
            verify(mapper, never()).countByWorkflowCode(any(), any(), any(), any());
    }
}