package com.btsheng.erp.business.crm.procurementapproval;

import com.btsheng.erp.business.crm.procurementapproval.dto.ApprovalRouteRequest;
import com.btsheng.erp.business.crm.procurementapproval.dto.ApprovalRouteResponse;
import com.btsheng.erp.business.crm.procurementapproval.service.ProcurementApprovalRouter;
import com.btsheng.erp.business.crm.workflowevent.service.WorkflowEventService;
import com.btsheng.erp.core.model.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * V1.3.8 · Story 4.2 · ProcurementApprovalRouter 单元测例
 *
 * <p>precheck 校正后（2026-06-13）：仅金额阈值走 sys_workflow_node，
 * 品类/紧急度走应用层 if 判断。详见 docs/dev/logs/4.2-precheck.log。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@DisplayName("Story 4.2 · ProcurementApprovalRouter 单元测例（V1.3.8 Sprint 7）")
class ProcurementApprovalRouterTest {

    private static Validator validator;
    private ProcurementApprovalRouter router;
    private WorkflowEventService workflowEventService;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        // V1.3.8 Sprint 9 Story 9.1：mock WorkflowEventService
            workflowEventService = mock(WorkflowEventService.class);
        router = new ProcurementApprovalRouter(workflowEventService);
    }

    // ==================== AC-4.2.2 路由决策矩阵 ====================
            @Test
    @DisplayName("AC-4.2.2.a 金额 5000 ≤ 1万 → SELF（空路由）")
    void route_below_10k_self() {
        ApprovalRouteRequest req = makeRequest("5000", null, null, null);
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        assertTrue(r.isSuccess() || r.getCode() == 0);
        assertEquals(0, r.getData().getRoute().size());
        assertTrue(r.getData().getMatchedThresholds().contains("AMOUNT_BELOW_10K"));
    }

    @Test
    @DisplayName("AC-4.2.2.b 金额 30000 → PROCUREMENT_MANAGER")
    void route_30k_pm() {
        ApprovalRouteRequest req = makeRequest("30000", null, null, null);
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        assertTrue(r.getData().getRoute().contains("PROCUREMENT_MANAGER"));
        assertTrue(r.getData().getMatchedThresholds().contains("AMOUNT_10K_50K"));
    }

    @Test
    @DisplayName("AC-4.2.2.c 金额 80000 > 5万 → GM + PROCUREMENT_MANAGER 双签")
    void route_above_50k_gm_pm() {
        ApprovalRouteRequest req = makeRequest("80000", null, null, null);
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        assertEquals(2, r.getData().getRoute().size());
        assertTrue(r.getData().getRoute().contains("GM"));
        assertTrue(r.getData().getRoute().contains("PROCUREMENT_MANAGER"));
        assertEquals(2, r.getData().getEstimatedSigners());
    }

    @Test
    @DisplayName("AC-4.2.2.d 金额边界 10000 → PROCUREMENT_MANAGER（>= 阈值）")
    void route_boundary_10k() {
        ApprovalRouteRequest req = makeRequest("10000", null, null, null);
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        // 边界：amount.compareTo(10000) < 0 = false → 走 PM 路由
            assertTrue(r.getData().getRoute().contains("PROCUREMENT_MANAGER"));
    }

    @Test
    @DisplayName("AC-4.2.2.e 金额边界 50000 → PROCUREMENT_MANAGER（< 阈值）")
    void route_boundary_50k() {
        ApprovalRouteRequest req = makeRequest("50000", null, null, null);
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        // 边界：amount.compareTo(50000) < 0 = false → 走 GM+PM
            assertTrue(r.getData().getRoute().contains("GM"));
    }

    // ==================== 品类分支（应用层 if · precheck 校正后） ====================
            @Test
    @DisplayName("AC-4.2.2.f 品类 TOOL → PROCUREMENT_MANAGER")
    void route_category_tool() {
        ApprovalRouteRequest req = makeRequest("30000", "TOOL", null, null);
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        assertTrue(r.getData().getRoute().contains("PROCUREMENT_MANAGER"));
        assertTrue(r.getData().getMatchedThresholds().contains("CATEGORY_TOOL"));
    }

    @Test
    @DisplayName("AC-4.2.2.g 品类 CHEMICAL → PROCUREMENT_MANAGER")
    void route_category_chemical() {
        ApprovalRouteRequest req = makeRequest("30000", "CHEMICAL", null, null);
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        assertTrue(r.getData().getRoute().contains("PROCUREMENT_MANAGER"));
        assertTrue(r.getData().getMatchedThresholds().contains("CATEGORY_CHEMICAL"));
    }

    @Test
    @DisplayName("AC-4.2.2.h 品类 MECHANICAL 不触发额外路由")
    void route_category_mechanical_noop() {
        ApprovalRouteRequest req = makeRequest("30000", "MECHANICAL", null, null);
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        assertFalse(r.getData().getMatchedThresholds().stream().anyMatch(s -> s.startsWith("CATEGORY_")));
    }

    // ==================== 紧急度分支 ====================
            @Test
    @DisplayName("AC-4.2.2.i 紧急度 URGENT + 金额 20000 → PROCUREMENT_MANAGER")
    void route_urgency_high_amount() {
        ApprovalRouteRequest req = makeRequest("20000", null, null, "URGENT");
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        assertTrue(r.getData().getRoute().contains("PROCUREMENT_MANAGER"));
        assertTrue(r.getData().getMatchedThresholds().contains("URGENCY_URGENT_AMOUNT_OVER_10K"));
    }

    @Test
    @DisplayName("AC-4.2.2.j 紧急度 URGENT + 金额 5000（≤ 1万）不触发")
    void route_urgency_high_but_below_10k() {
        ApprovalRouteRequest req = makeRequest("5000", null, null, "URGENT");
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        assertFalse(r.getData().getMatchedThresholds().contains("URGENCY_URGENT_AMOUNT_OVER_10K"));
    }

    @Test
    @DisplayName("AC-4.2.2.k 紧急度 NORMAL 不触发紧急路由")
    void route_urgency_normal_noop() {
        ApprovalRouteRequest req = makeRequest("30000", null, null, "NORMAL");
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        assertFalse(r.getData().getMatchedThresholds().stream().anyMatch(s -> s.startsWith("URGENCY_")));
    }

    // ==================== 兼容 legacy ====================
            @Test
    @DisplayName("AC-4.2.2.l 金额 30000 → compatibleLegacyRoute 含 DEPT_MANAGER")
    void route_legacy_dept_manager() {
        ApprovalRouteRequest req = makeRequest("30000", null, null, null);
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        assertNotNull(r.getData().getCompatibleLegacyRoute());
        assertTrue(r.getData().getCompatibleLegacyRoute().contains("DEPT_MANAGER"));
    }

    @Test
    @DisplayName("AC-4.2.2.m 金额 > 5万 → compatibleLegacyRoute 含 GM")
    void route_legacy_gm() {
        ApprovalRouteRequest req = makeRequest("80000", null, null, null);
        Result<ApprovalRouteResponse> r = router.previewRoute(req);
        assertTrue(r.getData().getCompatibleLegacyRoute().contains("GM"));
    }

    // ==================== 参数校验 ====================
            @Test
    @DisplayName("AC-4.2.2.n amount 必填")
    void route_amount_required() {
        ApprovalRouteRequest req = new ApprovalRouteRequest();
        Set<ConstraintViolation<ApprovalRouteRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
    }

    @Test
    @DisplayName("AC-4.2.2.o null request → fail")
    void route_null_request() {
        Result<ApprovalRouteResponse> r = router.previewRoute(null);
        assertFalse(r.isSuccess() || r.getCode() == 0);
    }

    // ==================== AC-4.2.1 权限查询 ====================
            @Test
    @DisplayName("AC-4.2.1.a PROCUREMENT_MANAGER 权限列表 4 项")
    void perms_4_items() {
        Result<List<String>> r = router.getProcurementManagerPermissions();
        assertEquals(4, r.getData().size());
        assertTrue(r.getData().contains("purchase:approval:read"));
        assertTrue(r.getData().contains("purchase:approval:approve"));
        assertTrue(r.getData().contains("purchase:approval:reject"));
        assertTrue(r.getData().contains("purchase:no-order:create"));
    }

    // ==================== Sprint 9 Story 9.1 · AC-9.1.2 workflow_event 触发（4 测例）====================
            @Test
    @DisplayName("9.1.2.a 路由预览触发 PREVIEWED workflow_event")
    void trigger_workflow_event_previewed() {
        router.previewRoute(makeRequest("30000", null, null, null));

        verify(workflowEventService, Mockito.times(1)).recordEvent(
                Mockito.eq("PO_APPROVAL"),
                Mockito.isNull(),     // bizId（无业务 ID）
            Mockito.isNull(),     // bizNo
                Mockito.eq("PREVIEWED"),
                Mockito.eq("PROCUREMENT_MANAGER"),  // route 第一个角色
            Mockito.isNull(), Mockito.isNull(),
                Mockito.anyString(),   // comment 含 amount
            Mockito.eq(5),         // matchedNodeIndex（10K-50K 节点）
                Mockito.eq("AMOUNT_10K_50K"));
    }

    @Test
    @DisplayName("9.1.2.b > 5万预览触发 PREVIEWED（matchedNodeIndex=6）")
    void trigger_previewed_amount_above_50k() {
        router.previewRoute(makeRequest("80000", null, null, null));

        verify(workflowEventService).recordEvent(
                Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.eq("PREVIEWED"),
                Mockito.eq("PROCUREMENT_MANAGER"),
                Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.eq(6), Mockito.eq("AMOUNT_ABOVE_50K"));
    }

    @Test
    @DisplayName("9.1.2.c ≤ 1万预览 SELF → matchedNodeIndex=null")
    void trigger_previewed_self_route() {
        router.previewRoute(makeRequest("5000", null, null, null));

        verify(workflowEventService).recordEvent(
                Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.eq("PREVIEWED"),
                Mockito.eq("SELF"),
                Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.isNull(), Mockito.eq("AMOUNT_BELOW_10K"));
    }

    @Test
    @DisplayName("9.1.2.d recordEvent 异常不影响主流程")
    void preview_event_exception_doesnt_break() {
        Mockito.doThrow(new RuntimeException("DB error"))
                .when(workflowEventService).recordEvent(
                        Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Result<ApprovalRouteResponse> r = router.previewRoute(makeRequest("30000", null, null, null));

        assertTrue(r.isSuccess() || r.getCode() == 0, "recordEvent 异常不应影响路由决策");
    }

    // ==================== 辅助 ====================
            private ApprovalRouteRequest makeRequest(String amount, String category, String supplierStatus, String urgency) {
        ApprovalRouteRequest req = new ApprovalRouteRequest();
        req.setAmount(new BigDecimal(amount));
        req.setCategory(category);
        req.setSupplierStatus(supplierStatus);
        req.setUrgency(urgency);
        return req;
    }
}