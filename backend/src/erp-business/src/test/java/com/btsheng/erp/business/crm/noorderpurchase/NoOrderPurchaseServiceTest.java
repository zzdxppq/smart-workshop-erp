package com.btsheng.erp.business.crm.noorderpurchase;

import com.btsheng.erp.business.crm.gmsummary.service.GmSummaryService;
import com.btsheng.erp.business.crm.noorderpurchase.dto.NoOrderPurchaseRequest;
import com.btsheng.erp.business.crm.noorderpurchase.dto.NoOrderPurchaseResponse;
import com.btsheng.erp.business.crm.noorderpurchase.dto.PurchaseReasonDto;
import com.btsheng.erp.business.crm.noorderpurchase.enums.PurchaseReason;
import com.btsheng.erp.business.crm.noorderpurchase.enums.PurchaseSourceType;
import com.btsheng.erp.business.crm.noorderpurchase.service.NoOrderPurchaseService;
import com.btsheng.erp.business.crm.purchaseorder.entity.CrmPurchaseOrder;
import com.btsheng.erp.business.crm.purchaseorder.mapper.CrmPurchaseOrderMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.rfq.mapper.CrmRfqVendorMapper;
import com.btsheng.erp.business.crm.workflowevent.service.WorkflowEventService;
import com.btsheng.erp.core.model.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V1.3.8 · Story 4.1 · NoOrderPurchaseService 单元测例
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@DisplayName("Story 4.1 · NoOrderPurchaseService 单元测例（V1.3.8 Sprint 7）")
class NoOrderPurchaseServiceTest {

    private static Validator validator;
    private NoOrderPurchaseService service;
    private CrmPurchaseOrderMapper purchaseOrderMapper;
    private DocNoGenerator docNoGenerator;
    private GmSummaryService gmSummaryService;
    private WorkflowEventService workflowEventService;
    private CrmRfqVendorMapper rfqVendorMapper;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        // V1.3.8 Sprint 7 集成 C + Sprint 9 Story 9.1：mock 真实依赖
            purchaseOrderMapper = mock(CrmPurchaseOrderMapper.class);
        docNoGenerator = mock(DocNoGenerator.class);
        gmSummaryService = mock(GmSummaryService.class);
        workflowEventService = mock(WorkflowEventService.class);
        rfqVendorMapper = mock(CrmRfqVendorMapper.class);

        // mock DocNoGenerator 返回固定值
            Mockito.when(docNoGenerator.nextOrderNo()).thenReturn("XS-20260613-0001");

        // mock insert：设置 insert 后 ID 自动回填
            doAnswer(invocation -> {
            CrmPurchaseOrder po = invocation.getArgument(0);
            po.setId(8008L);
            return 1;
        }).when(purchaseOrderMapper).insert(any(CrmPurchaseOrder.class));

        service = new NoOrderPurchaseService(purchaseOrderMapper, docNoGenerator, gmSummaryService, workflowEventService, rfqVendorMapper);
    }

    // ==================== AC-4.1.1 无订单采购创建 ====================
            @Test
    @DisplayName("AC-4.1.1.a 完整请求创建成功")
    void createNoOrder_valid() {
        NoOrderPurchaseRequest req = makeValidRequest();
        Result<NoOrderPurchaseResponse> r = service.createNoOrderPurchase(req, 1001L);
        assertTrue(r.isSuccess() || r.getCode() == 0, "应成功");
        assertNotNull(r.getData());
        assertEquals("NO_ORDER", r.getData().getSourceType());
        assertEquals("URGENT_REPLENISH", r.getData().getPurchaseReason());
    }

    @Test
    @DisplayName("AC-4.1.1.b 聚合金额正确（quantity × estimatedPrice）")
    void createNoOrder_aggregated_total() {
        NoOrderPurchaseRequest req = makeValidRequest();
        // 50 × 80.0 = 4000
            Result<NoOrderPurchaseResponse> r = service.createNoOrderPurchase(req, 1001L);
        assertEquals(new BigDecimal("4000.00"), r.getData().getEstimatedTotal());
    }

    @Test
    @DisplayName("AC-4.1.1.c 金额 ≤ 1 万 路由 SELF")
    void createNoOrder_route_self_below10k() {
        NoOrderPurchaseRequest req = makeValidRequest(); // 4000
            Result<NoOrderPurchaseResponse> r = service.createNoOrderPurchase(req, 1001L);
        assertEquals("SELF", r.getData().getApprovalRoute());
    }

    @Test
    @DisplayName("AC-4.1.1.d 金额 1-5 万 路由 PROCUREMENT_MANAGER")
    void createNoOrder_route_pm_between() {
        NoOrderPurchaseRequest req = makeValidRequest();
        // 200 × 100.0 = 20000 → PROCUREMENT_MANAGER
            req.getItems().get(0).setQuantity(200);
        req.getItems().get(0).setEstimatedPrice(new BigDecimal("100.00"));
        Result<NoOrderPurchaseResponse> r = service.createNoOrderPurchase(req, 1001L);
        assertEquals(NoOrderPurchaseService.ROUTE_PM, r.getData().getApprovalRoute());
    }

    @Test
    @DisplayName("AC-4.1.1.e 金额 > 5 万 路由 GM+PROCUREMENT_MANAGER")
    void createNoOrder_route_gm_pm_above50k() {
        NoOrderPurchaseRequest req = makeValidRequest();
        // 1000 × 100.0 = 100000 → GM+PM
            req.getItems().get(0).setQuantity(1000);
        req.getItems().get(0).setEstimatedPrice(new BigDecimal("100.00"));
        Result<NoOrderPurchaseResponse> r = service.createNoOrderPurchase(req, 1001L);
        assertEquals(NoOrderPurchaseService.ROUTE_GM_PM, r.getData().getApprovalRoute());
    }

    @Test
    @DisplayName("AC-4.1.1.f 无效 purchase_reason 拒绝")
    void createNoOrder_invalid_reason() {
        NoOrderPurchaseRequest req = makeValidRequest();
        req.setPurchaseReason("INVALID_REASON");
        Result<NoOrderPurchaseResponse> r = service.createNoOrderPurchase(req, 1001L);
        assertFalse(r.isSuccess() || r.getCode() == 0, "无效理由应失败");
    }

    @Test
    @DisplayName("AC-4.1.1.g null request 拒绝")
    void createNoOrder_null_request() {
        Result<NoOrderPurchaseResponse> r = service.createNoOrderPurchase(null, 1001L);
        assertFalse(r.isSuccess() || r.getCode() == 0);
    }

    // ==================== AC-4.1.2 字典查询 ====================
            @Test
    @DisplayName("AC-4.1.2.a 返回 4 项字典")
    void listReasons_count_4() {
        Result<List<PurchaseReasonDto>> r = service.listPurchaseReasons();
        assertEquals(4, r.getData().size());
    }

    @Test
    @DisplayName("AC-4.1.2.b 包含所有 4 枚举 code")
    void listReasons_all_codes() {
        Result<List<PurchaseReasonDto>> r = service.listPurchaseReasons();
        List<String> codes = r.getData().stream().map(PurchaseReasonDto::getCode).sorted().toList();
        assertEquals(List.of("CUSTOMER_ADD", "OTHER", "STOCK_SWAP", "URGENT_REPLENISH"), codes);
    }

    @Test
    @DisplayName("AC-4.1.2.c 每项含 name + color")
    void listReasons_fields() {
        Result<List<PurchaseReasonDto>> r = service.listPurchaseReasons();
        PurchaseReasonDto urgent = r.getData().stream()
                .filter(x -> "URGENT_REPLENISH".equals(x.getCode())).findFirst().orElse(null);
        assertNotNull(urgent);
        assertEquals("紧急补料", urgent.getName());
        assertEquals("red", urgent.getColor());
    }

    // ==================== 集成 C：真实 INSERT crm_purchase_order ====================
            @Test
    @DisplayName("集成 C.a 真实 INSERT crm_purchase_order")
    void insert_crm_purchase_order() {
        NoOrderPurchaseRequest req = makeValidRequest();
        service.createNoOrderPurchase(req, 1001L);

        ArgumentCaptor<CrmPurchaseOrder> captor = ArgumentCaptor.forClass(CrmPurchaseOrder.class);
        verify(purchaseOrderMapper).insert(captor.capture());

        CrmPurchaseOrder inserted = captor.getValue();
        assertEquals("XS-20260613-0001", inserted.getPoNo());
        assertEquals("NO_ORDER", inserted.getSourceType());
        assertEquals("URGENT_REPLENISH", inserted.getPurchaseReason());
        assertEquals("SELF", inserted.getApprovalRoute());
        assertEquals("PENDING", inserted.getApprovalStatus());
        assertEquals("PENDING_SHIP", inserted.getStatus());
        assertNull(inserted.getRfqId(), "NO_ORDER 模式 rfq_id 必为 null");
    }

    @Test
    @DisplayName("集成 C.b gm:summary 缓存失效被调用")
    void evict_gm_summary_cache() {
        NoOrderPurchaseRequest req = makeValidRequest();
        // 非事务场景下 evictCache 直接调用
            service.createNoOrderPurchase(req, 1001L);

        // 非事务场景：直接清缓存
            verify(gmSummaryService).evictCache();
    }

    @Test
    @DisplayName("集成 C.c 金额 > 1 万 → approval_route=PROCUREMENT_MANAGER")
    void insert_amount_30k_route_pm() {
        NoOrderPurchaseRequest req = makeValidRequest();
        req.getItems().get(0).setQuantity(200);
        req.getItems().get(0).setEstimatedPrice(new BigDecimal("100.00"));

        service.createNoOrderPurchase(req, 1001L);

        ArgumentCaptor<CrmPurchaseOrder> captor = ArgumentCaptor.forClass(CrmPurchaseOrder.class);
        verify(purchaseOrderMapper).insert(captor.capture());
        assertEquals(NoOrderPurchaseService.ROUTE_PM, captor.getValue().getApprovalRoute());
    }

    // ==================== Sprint 9 Story 9.1 · AC-9.1.1 workflow_event 触发（4 测例）====================
            @Test
    @DisplayName("9.1.1.a NO_ORDER 创建后触发 CREATED workflow_event")
    void trigger_workflow_event_created() {
        service.createNoOrderPurchase(makeValidRequest(), 1001L);

        verify(workflowEventService, Mockito.times(1)).recordEvent(
                Mockito.eq("PO_APPROVAL"),
                Mockito.any(Long.class),
                Mockito.anyString(),
                Mockito.eq(WorkflowEventService.EVENT_CREATED),
                Mockito.anyString(),  // 初始路由（SELF / PROCUREMENT_MANAGER / ...）
            Mockito.isNull(),     // approverUserId（NULL，PO 尚未分配审批人）
                Mockito.isNull(),
                Mockito.anyString(),  // comment 含 purchase_reason
            Mockito.isNull(),     // matchedNodeIndex
                Mockito.isNull()      // matchedThreshold
        );
    }

    @Test
    @DisplayName("9.1.1.b workflow_event biz_id 与 PO id 一致")
    void workflow_event_biz_id_matches_po() {
        service.createNoOrderPurchase(makeValidRequest(), 1001L);

        ArgumentCaptor<Long> bizIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(workflowEventService).recordEvent(
                Mockito.eq("PO_APPROVAL"),
                bizIdCaptor.capture(),
                Mockito.anyString(),
                Mockito.eq(WorkflowEventService.EVENT_CREATED),
                Mockito.anyString(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        assertEquals(8008L, bizIdCaptor.getValue());
    }

    @Test
    @DisplayName("9.1.1.c workflow_event comment 含 purchase_reason")
    void workflow_event_comment_contains_reason() {
        service.createNoOrderPurchase(makeValidRequest(), 1001L);

        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
        verify(workflowEventService).recordEvent(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(),
                commentCaptor.capture(),
                Mockito.any(), Mockito.any());

        assertTrue(commentCaptor.getValue().contains("URGENT_REPLENISH"));
    }

    @Test
    @DisplayName("9.1.1.d workflow_event 异常不影响主流程")
    void workflow_event_exception_doesnt_break_main_flow() {
        Mockito.doThrow(new RuntimeException("DB connection lost"))
                .when(workflowEventService).recordEvent(
                        Mockito.anyString(), Mockito.any(), Mockito.any(),
                        Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any());

        // 即使 recordEvent 失败，createNoOrderPurchase 仍应成功
            NoOrderPurchaseRequest req = makeValidRequest();
        Result<NoOrderPurchaseResponse> r = service.createNoOrderPurchase(req, 1001L);

        assertTrue(r.isSuccess() || r.getCode() == 0, "recordEvent 异常不应影响 PO 创建");
        assertEquals(8008L, r.getData().getPoId());
    }

    // ==================== Enum + DTO 校验 ====================
            @Test
    @DisplayName("PurchaseSourceType 3 枚举完整")
    void sourceType_enum_3_values() {
        assertEquals(3, PurchaseSourceType.values().length);
        assertEquals("FROM_ORDER", PurchaseSourceType.FROM_ORDER.getCode());
        assertEquals("FROM_MRP", PurchaseSourceType.FROM_MRP.getCode());
        assertEquals("NO_ORDER", PurchaseSourceType.NO_ORDER.getCode());
    }

    @Test
    @DisplayName("PurchaseReason.fromCode 正常解析 + null 兜底")
    void reason_fromCode() {
        assertEquals(PurchaseReason.URGENT_REPLENISH, PurchaseReason.fromCode("URGENT_REPLENISH"));
        assertNull(PurchaseReason.fromCode("NOT_EXIST"));
    }

    @Test
    @DisplayName("Request purchaseReason 必填校验")
    void request_purchaseReason_required() {
        NoOrderPurchaseRequest req = makeValidRequest();
        req.setPurchaseReason(null);
        Set<ConstraintViolation<NoOrderPurchaseRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
    }

    @Test
    @DisplayName("Request items 必填校验")
    void request_items_notEmpty() {
        NoOrderPurchaseRequest req = makeValidRequest();
        req.setItems(new ArrayList<>());
        Set<ConstraintViolation<NoOrderPurchaseRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
    }

    @Test
    @DisplayName("Item estimatedPrice >= 0.01 校验")
    void item_estimatedPrice_min() {
        NoOrderPurchaseRequest req = makeValidRequest();
        req.getItems().get(0).setEstimatedPrice(BigDecimal.ZERO);
        Set<ConstraintViolation<NoOrderPurchaseRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty(), "estimatedPrice 必须 >= 0.01");
    }

    // ==================== 辅助方法 ====================
            private NoOrderPurchaseRequest makeValidRequest() {
        NoOrderPurchaseRequest req = new NoOrderPurchaseRequest();
        req.setPurchaseReason("URGENT_REPLENISH");
        req.setSupplierId(2001L);
        req.setRemark("生产中途 M1 损坏 5 件，紧急补料");

        NoOrderPurchaseRequest.Item item = new NoOrderPurchaseRequest.Item();
        item.setMaterialId(5001L);
        item.setQuantity(50);
        item.setEstimatedPrice(new BigDecimal("80.00"));
        req.setItems(List.of(item));

        return req;
    }
}