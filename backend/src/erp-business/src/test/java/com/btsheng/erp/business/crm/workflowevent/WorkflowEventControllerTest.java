package com.btsheng.erp.business.crm.workflowevent;

import com.btsheng.erp.business.crm.workflowevent.controller.WorkflowEventController;
import com.btsheng.erp.business.crm.workflowevent.dto.WorkflowEventStatsDTO;
import com.btsheng.erp.business.crm.workflowevent.service.WorkflowEventService;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V1.3.8 Sprint 10 Story 10.3 · WorkflowEventController 4 测例
 *
 * <p>覆盖：byEventType / byApproverRole / 时间范围 / 权限（占位，由 SecurityConfig 集成测试兜底）
 *
 * @author dev agent Opus 4.8 · 2026-06-14
 */
@DisplayName("Story 10.3 · WorkflowEventController 测例")
class WorkflowEventControllerTest {

    private WorkflowEventService service;
    private WorkflowEventController controller;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(WorkflowEventService.class);
        controller = new WorkflowEventController(service);
    }

    /** TC-10.3.1.1 byEventType 分组 + 完整 4 字段响应 */
    @Test
    @DisplayName("TC-10.3.1.1 byEventType 分组 5/3/2 + totalCount=10")
    void stats_by_event_type_grouping() {
        WorkflowEventStatsDTO dto = new WorkflowEventStatsDTO();
        dto.setTotalCount(10L);
        Map<String, Long> byType = new HashMap<>();
        byType.put("CREATED", 5L);
        byType.put("APPROVED", 3L);
        byType.put("REJECTED", 2L);
        dto.setByEventType(byType);
        WorkflowEventStatsDTO.Period p = new WorkflowEventStatsDTO.Period();
        p.setStartDate(LocalDate.of(2026, 5, 14));
        p.setEndDate(LocalDate.of(2026, 6, 13));
        dto.setPeriod(p);
        Mockito.when(service.stats(Mockito.eq("PO_APPROVAL"), Mockito.isNull(), Mockito.isNull(), Mockito.isNull()))
                .thenReturn(Result.ok(dto));

        Result<WorkflowEventStatsDTO> r = controller.stats("PO_APPROVAL", null, null, null);

        assertTrue(r.isSuccess() || r.getCode() == 0);
        assertEquals(10L, r.getData().getTotalCount());
        assertEquals(5L, r.getData().getByEventType().get("CREATED"));
        assertEquals(3L, r.getData().getByEventType().get("APPROVED"));
        assertEquals(2L, r.getData().getByEventType().get("REJECTED"));
        assertNotNull(r.getData().getPeriod());
    }

    /** TC-10.3.2.2 byApproverRole 复合角色值（GM+PROCUREMENT_MANAGER）不被拆分 */
    @Test
    @DisplayName("TC-10.3.2.2 复合角色值 GM+PROCUREMENT_MANAGER 不拆分")
    void stats_composite_role_not_split() {
        WorkflowEventStatsDTO dto = new WorkflowEventStatsDTO();
        dto.setTotalCount(3L);
        Map<String, Long> byRole = new HashMap<>();
        byRole.put("GM+PROCUREMENT_MANAGER", 3L);
        dto.setByApproverRole(byRole);
        Mockito.when(service.stats(Mockito.eq("PO_APPROVAL"),
                Mockito.eq("GM+PROCUREMENT_MANAGER"),
                Mockito.isNull(), Mockito.isNull()))
                .thenReturn(Result.ok(dto));

        Result<WorkflowEventStatsDTO> r = controller.stats("PO_APPROVAL",
                "GM+PROCUREMENT_MANAGER", null, null);

        assertTrue(r.isSuccess() || r.getCode() == 0);
        assertEquals(3L, r.getData().getByApproverRole().get("GM+PROCUREMENT_MANAGER"));
        // 关键：必须是单一 key，不能被 split
            assertEquals(1, r.getData().getByApproverRole().size());
    }

    /** TC-10.3.3.3 时间范围 start_date > end_date 边界 → 40001 */
    @Test
    @DisplayName("TC-10.3.3.3 start_date > end_date → 40001")
    void stats_start_after_end_returns_40001() {
        LocalDate start = LocalDate.of(2026, 6, 13);
        LocalDate end = LocalDate.of(2026, 5, 1);
        Mockito.when(service.stats(Mockito.eq("PO_APPROVAL"), Mockito.isNull(),
                Mockito.eq(start), Mockito.eq(end)))
                .thenReturn(Result.fail(Result.CODE_PARAM_MISSING, "start_date 必须 ≤ end_date"));

        Result<WorkflowEventStatsDTO> r = controller.stats("PO_APPROVAL", null, start, end);

        assertFalse(r.isSuccess() || r.getCode() == 0);
        assertEquals(40001, r.getCode());
    }

    /** TC-10.3.4.1 权限占位 — Service 层无法测 @PreAuthorize，由 SecurityConfig IT 覆盖（记录在 dev log） */
    @Test
    @DisplayName("TC-10.3.4.1 权限元数据：方法标注 hasAnyRole GM, ADMIN（通过注解反射验证）")
    void stats_preauthorize_annotation_present() {
        try {
            java.lang.reflect.Method m = WorkflowEventController.class.getMethod("stats",
                    String.class, String.class, LocalDate.class, LocalDate.class);
            org.springframework.security.access.prepost.PreAuthorize ann =
                    m.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
            assertNotNull(ann, "@PreAuthorize 注解必须存在");
            assertTrue(ann.value().contains("GM") && ann.value().contains("ADMIN"),
                    "权限表达式必须包含 GM + ADMIN，实际：" + ann.value());
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Controller 缺少 stats 方法签名", e);
        }
    }
}
