package com.btsheng.erp.business.crm.rfq.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.rfq.dto.AddRfqVendorRequest;
import com.btsheng.erp.business.crm.rfq.dto.AwardRfqRequest;
import com.btsheng.erp.business.crm.rfq.dto.CreateRfqRequest;
import com.btsheng.erp.business.crm.rfq.dto.SubmitQuoteRequest;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfq;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfqQuote;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfqVendor;
import com.btsheng.erp.business.crm.rfq.mapper.CrmRfqMapper;
import com.btsheng.erp.business.crm.rfq.mapper.CrmRfqQuoteMapper;
import com.btsheng.erp.business.crm.rfq.mapper.CrmRfqVendorMapper;
import com.btsheng.erp.business.crm.workflowevent.service.WorkflowEventService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.32 · 采购·询比价 Service 单元测试（FR-8-1）
 * 14 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RfqServiceTest {

    @Mock private CrmRfqMapper rfqMapper;
    @Mock private CrmRfqVendorMapper vendorMapper;
    @Mock private CrmRfqQuoteMapper quoteMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private RfqService service;

    // V1.3.8 Sprint 9 Story 9.1：mock WorkflowEventService
            @Mock
    private WorkflowEventService workflowEventService;

    @BeforeEach
    void setUp() {
        service = new RfqService(rfqMapper, vendorMapper, quoteMapper, docNoGenerator, workflowEventService);
        when(docNoGenerator.nextRfqNo())
                .thenReturn("RF20260612-0001", "RF20260612-0002", "RF20260612-0003", "RF20260612-0004");
        when(docNoGenerator.nextNo("PO"))
                .thenReturn("PO20260612-0001", "PO20260612-0002", "PO20260612-0003");
        when(rfqMapper.insert(any(CrmRfq.class))).thenAnswer(inv -> {
            CrmRfq r = inv.getArgument(0);
            r.setId(1L);
            return 1;
        });
        when(vendorMapper.insert(any(CrmRfqVendor.class))).thenAnswer(inv -> {
            CrmRfqVendor v = inv.getArgument(0);
            v.setId(1L);
            return 1;
        });
        when(quoteMapper.insert(any(CrmRfqQuote.class))).thenAnswer(inv -> {
            CrmRfqQuote q = inv.getArgument(0);
            q.setId(1L);
            return 1;
        });
        when(rfqMapper.updateById(any(CrmRfq.class))).thenReturn(1);
        when(vendorMapper.updateById(any(CrmRfqVendor.class))).thenReturn(1);
        when(quoteMapper.updateById(any(CrmRfqQuote.class))).thenReturn(1);
    }

    private CreateRfqRequest buildValidReq() {
        CreateRfqRequest r = new CreateRfqRequest();
        r.setTitle("6061 铝板 100 套");
        r.setMaterialId(1001L);
        r.setMaterialCode("M-AL6061-PT");
        r.setQty(new BigDecimal("100"));
        r.setUnit("PCS");
        r.setBudgetAmount(new BigDecimal("50000"));
        r.setWinnerMode("LOWEST");
        return r;
    }

    private AddRfqVendorRequest buildVendorReq(Long vendorId, String name) {
        AddRfqVendorRequest v = new AddRfqVendorRequest();
        v.setVendorId(vendorId);
        v.setVendorName(name);
        v.setContactName("张" + name);
        v.setContactPhone("13800000000");
        return v;
    }

    private SubmitQuoteRequest buildQuoteReq(Long vendorId, String unitPrice, String total) {
        SubmitQuoteRequest q = new SubmitQuoteRequest();
        q.setVendorId(vendorId);
        q.setUnitPrice(new BigDecimal(unitPrice));
        q.setTotalAmount(new BigDecimal(total));
        q.setLeadTimeDays(7);
        q.setQualityScore(new BigDecimal("4.5"));
        q.setDeliveryScore(new BigDecimal("4.0"));
        q.setServiceScore(new BigDecimal("4.0"));
        return q;
    }

    // ====== createRfq 4 测例 ======
            @Test
    @DisplayName("createRfq happy path · 单号 RF 前缀")
    void testCreate_OK() {
        Result<CrmRfq> r = service.createRfq(buildValidReq(), 501L);
        assertEquals(0, r.getCode());
        assertEquals("RF20260612-0001", r.getData().getRfqNo());
        assertEquals("DRAFT", r.getData().getStatus());
    }

    @Test
    @DisplayName("createRfq 缺标题 → 40001")
    void testCreate_TitleRequired() {
        CreateRfqRequest r = buildValidReq();
        r.setTitle(null);
        Result<CrmRfq> result = service.createRfq(r, 501L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("createRfq 缺物料 ID → 40001")
    void testCreate_MaterialRequired() {
        CreateRfqRequest r = buildValidReq();
        r.setMaterialId(null);
        Result<CrmRfq> result = service.createRfq(r, 501L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 3：预算负数 → 40001")
    void testCreate_BudgetNegative() {
        CreateRfqRequest r = buildValidReq();
        r.setBudgetAmount(new BigDecimal("-100"));
        Result<CrmRfq> result = service.createRfq(r, 501L);
        assertEquals(40001, result.getCode());
        assertEquals("BUDGET_NEGATIVE", result.getMessage());
    }

    // ====== addVendor 3 测例 ======
            @Test
    @DisplayName("addVendor happy path · DRAFT → QUOTING")
    void testAddVendor_OK() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(1L);
        rfq.setStatus("DRAFT");
        when(rfqMapper.selectById(1L)).thenReturn(rfq);
        when(vendorMapper.selectByRfqAndVendor(1L, 901L)).thenReturn(null);

        Result<CrmRfqVendor> r = service.addVendor(1L, buildVendorReq(901L, "上海铝业"), 501L);
        assertEquals(0, r.getCode());
        assertEquals("PENDING", r.getData().getQuoteStatus());
        assertEquals("QUOTING", rfq.getStatus());
    }

    @Test
    @DisplayName("P1 修补 2：同厂商重复 → 40902")
    void testAddVendor_Duplicate() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(1L);
        rfq.setStatus("QUOTING");
        when(rfqMapper.selectById(1L)).thenReturn(rfq);
        CrmRfqVendor existing = new CrmRfqVendor();
        when(vendorMapper.selectByRfqAndVendor(1L, 901L)).thenReturn(existing);

        Result<CrmRfqVendor> r = service.addVendor(1L, buildVendorReq(901L, "上海铝业"), 501L);
        assertEquals(40902, r.getCode());
    }

    @Test
    @DisplayName("addVendor AWARDED 状态 → 40903")
    void testAddVendor_Locked() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(1L);
        rfq.setStatus("AWARDED");
        when(rfqMapper.selectById(1L)).thenReturn(rfq);

        Result<CrmRfqVendor> r = service.addVendor(1L, buildVendorReq(901L, "上海铝业"), 501L);
        assertEquals(40903, r.getCode());
    }

    // ====== submitQuote 3 测例 ======
            @Test
    @DisplayName("submitQuote happy path · 加权分自动计算")
    void testSubmitQuote_OK() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(1L);
        rfq.setStatus("QUOTING");
        when(rfqMapper.selectById(1L)).thenReturn(rfq);
        CrmRfqVendor rv = new CrmRfqVendor();
        rv.setId(10L);
        when(vendorMapper.selectByRfqAndVendor(1L, 901L)).thenReturn(rv);
        when(quoteMapper.selectByRfqAndVendor(1L, 901L)).thenReturn(null);

        Result<CrmRfqQuote> r = service.submitQuote(1L, buildQuoteReq(901L, "450", "45000"), 501L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().getWeightedScore());
    }

    @Test
    @DisplayName("P1 修补 2：单价 ≤ 0 → 40001")
    void testSubmitQuote_UnitPriceInvalid() {
        Result<CrmRfqQuote> r = service.submitQuote(1L, buildQuoteReq(901L, "0", "45000"), 501L);
        assertEquals(40001, r.getCode());
        assertEquals("UNIT_PRICE_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("P1 修补 2：总价 ≤ 0 → 40001")
    void testSubmitQuote_TotalInvalid() {
        Result<CrmRfqQuote> r = service.submitQuote(1L, buildQuoteReq(901L, "450", "0"), 501L);
        assertEquals(40001, r.getCode());
        assertEquals("TOTAL_AMOUNT_REQUIRED", r.getMessage());
    }

    // ====== compareQuotes 2 测例 ======
            @Test
    @DisplayName("compareQuotes LOWEST · 选最低价")
    void testCompare_Lowest() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(1L);
        rfq.setStatus("QUOTING");
        rfq.setBudgetAmount(new BigDecimal("50000"));
        rfq.setWinnerMode("LOWEST");
        when(rfqMapper.selectById(1L)).thenReturn(rfq);

        CrmRfqQuote q1 = new CrmRfqQuote();
        q1.setId(11L); q1.setRfqId(1L); q1.setVendorId(901L); q1.setTotalAmount(new BigDecimal("45000"));
        CrmRfqQuote q2 = new CrmRfqQuote();
        q2.setId(12L); q2.setRfqId(1L); q2.setVendorId(902L); q2.setTotalAmount(new BigDecimal("46000"));
        when(quoteMapper.selectByRfqId(1L)).thenReturn(List.of(q1, q2));

        Result<Map<String, Object>> r = service.compareQuotes(1L);
        assertEquals(0, r.getCode());
        CrmRfqQuote winner = (CrmRfqQuote) r.getData().get("winner");
        assertEquals(901L, winner.getVendorId());
        assertEquals(0, winner.getTotalAmount().compareTo(new BigDecimal("45000")));
    }

    @Test
    @DisplayName("P1 修补 3：选最低价超预算 → 40303")
    void testCompare_OverBudget() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(1L);
        rfq.setStatus("QUOTING");
        rfq.setBudgetAmount(new BigDecimal("10000"));   // 预算太小
            rfq.setWinnerMode("LOWEST");
        when(rfqMapper.selectById(1L)).thenReturn(rfq);

        CrmRfqQuote q1 = new CrmRfqQuote();
        q1.setId(11L); q1.setVendorId(901L); q1.setTotalAmount(new BigDecimal("45000"));
        when(quoteMapper.selectByRfqId(1L)).thenReturn(List.of(q1));

        Result<Map<String, Object>> r = service.compareQuotes(1L);
        assertEquals(40303, r.getCode());
    }

    // ====== awardRfq 2 测例 ======
            @Test
    @DisplayName("awardRfq · P1 修补 4 自动触发 PO 闭环")
    void testAward_OK() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(1L);
        rfq.setStatus("QUOTING");
        rfq.setBudgetAmount(new BigDecimal("50000"));
        rfq.setWinnerMode("LOWEST");
        rfq.setRfqNo("RF20260612-0001");
        when(rfqMapper.selectById(1L)).thenReturn(rfq);

        CrmRfqQuote q1 = new CrmRfqQuote();
        q1.setId(11L); q1.setRfqId(1L); q1.setVendorId(901L); q1.setTotalAmount(new BigDecimal("45000"));
        CrmRfqVendor rv = new CrmRfqVendor();
        rv.setId(10L); rv.setVendorId(901L); rv.setVendorName("上海铝业");
        when(quoteMapper.selectByRfqId(1L)).thenReturn(List.of(q1));
        when(vendorMapper.selectByRfqAndVendor(1L, 901L)).thenReturn(rv);

        AwardRfqRequest req = new AwardRfqRequest();
        req.setAutoCreatePo(true);

        Result<Map<String, Object>> r = service.awardRfq(1L, req, 501L);
        assertEquals(0, r.getCode());
        assertEquals("AWARDED", rfq.getStatus());
        assertEquals("PO20260612-0001", rfq.getPurchaseOrderNo());
    }

    @Test
    @DisplayName("awardRfq 重复中标 → 40903")
    void testAward_AlreadyAwarded() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(1L);
        rfq.setStatus("AWARDED");
        when(rfqMapper.selectById(1L)).thenReturn(rfq);

        Result<Map<String, Object>> r = service.awardRfq(1L, null, 501L);
        assertEquals(40903, r.getCode());
    }

    // ==================== Sprint 9 Story 9.1 · AC-9.1.3 RFQ 中标触发 AWARDED（4 测例）====================
            @Test
    @DisplayName("9.1.3.a RFQ 中标触发 AWARDED workflow_event")
    void trigger_workflow_event_awarded() {
        // 构造 compareQuotes 成功场景（避免抛 NO_WINNER）
            CrmRfq rfq = makeRfq(1L, "RF001");
        when(rfqMapper.selectById(1L)).thenReturn(rfq);
        when(rfqMapper.updateById(any(CrmRfq.class))).thenReturn(1);

        // 中标报价
            CrmRfqQuote winner = new CrmRfqQuote();
        winner.setId(100L);
        winner.setRfqId(1L);
        winner.setVendorId(2001L);
        winner.setTotalAmount(new BigDecimal("50000"));
        when(quoteMapper.selectByRfqId(1L)).thenReturn(List.of(winner));
        when(quoteMapper.updateById(any(CrmRfqQuote.class))).thenReturn(1);

        when(quoteMapper.selectAwardedByRfqId(1L)).thenReturn(winner);  // mapper 真实方法名  // 兼容旧 compareQuotes
            AwardRfqRequest req = new AwardRfqRequest();
        req.setAutoCreatePo(true);

        service.awardRfq(1L, req, 501L);

        verify(workflowEventService, times(1)).recordEvent(
                eq("QUOTE_APPROVAL"),
                eq(1L),
                eq("RF001"),
                eq("AWARDED"),
                eq("PURCHASER"),
                eq(501L),
                isNull(),
                anyString(),  // comment 含 "RFQ 中标"
            isNull(),
                isNull());
    }

    @Test
    @DisplayName("9.1.3.b 中标 comment 含 PO 号")
    void awarded_comment_contains_po_no() {
        CrmRfq rfq = makeRfq(1L, "RF001");
        when(rfqMapper.selectById(1L)).thenReturn(rfq);
        when(rfqMapper.updateById(any(CrmRfq.class))).thenReturn(1);

        CrmRfqQuote winner = new CrmRfqQuote();
        winner.setId(100L);
        winner.setRfqId(1L);
        winner.setVendorId(2001L);
        winner.setTotalAmount(new BigDecimal("50000"));
        when(quoteMapper.selectByRfqId(1L)).thenReturn(List.of(winner));
        when(quoteMapper.updateById(any(CrmRfqQuote.class))).thenReturn(1);
        when(quoteMapper.selectAwardedByRfqId(1L)).thenReturn(winner);  // mapper 真实方法名
            AwardRfqRequest req = new AwardRfqRequest();
        req.setAutoCreatePo(true);

        service.awardRfq(1L, req, 501L);

        org.mockito.ArgumentCaptor<String> captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(workflowEventService).recordEvent(
                any(), any(), any(), any(),
                any(), any(), any(),
                captor.capture(),
                any(), any());

        assertTrue(captor.getValue().contains("中标"), "comment 应含中标：" + captor.getValue());
    }

    @Test
    @DisplayName("9.1.3.c 中标 RFQ 不存在时不触发 event")
    void awarded_rfq_not_found_no_event() {
        when(rfqMapper.selectById(999L)).thenReturn(null);

        service.awardRfq(999L, null, 501L);

        verify(workflowEventService, never()).recordEvent(
                any(), any(), any(),
                any(), any(), any(), any(),
                any(), any(), any());
    }

    @Test
    @DisplayName("9.1.3.d recordEvent 异常不影响中标主流程")
    void awarded_event_exception_doesnt_break() {
        doThrow(new RuntimeException("DB error"))
                .when(workflowEventService).recordEvent(
                        anyString(), any(), any(), anyString(),
                        any(), any(), any(), any(), any(), any());

        CrmRfq rfq = makeRfq(1L, "RF001");
        when(rfqMapper.selectById(1L)).thenReturn(rfq);
        when(rfqMapper.updateById(any(CrmRfq.class))).thenReturn(1);

        CrmRfqQuote winner = new CrmRfqQuote();
        winner.setId(100L);
        winner.setRfqId(1L);
        winner.setVendorId(2001L);
        winner.setTotalAmount(new BigDecimal("50000"));
        when(quoteMapper.selectByRfqId(1L)).thenReturn(List.of(winner));
        when(quoteMapper.updateById(any(CrmRfqQuote.class))).thenReturn(1);
        when(quoteMapper.selectAwardedByRfqId(1L)).thenReturn(winner);  // mapper 真实方法名
            AwardRfqRequest req = new AwardRfqRequest();
        req.setAutoCreatePo(true);

        Result<Map<String, Object>> r = service.awardRfq(1L, req, 501L);

        assertTrue(r.isSuccess() || r.getCode() == 0, "recordEvent 异常不应影响中标");
    }

    /**
     * V1.3.8 Sprint 9 Story 9.1 测例辅助：构造测试 RFQ 实体
     */
    private CrmRfq makeRfq(Long id, String rfqNo) {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(id);
        rfq.setRfqNo(rfqNo);
        rfq.setStatus("COMPARED");
        rfq.setWinnerMode("LOWEST");  // V1.3.8 Sprint 9 Story 9.1：避免 WEIGHTED 分支 unitPrice null NPE
            rfq.setBudgetAmount(new BigDecimal("100000"));  // 防止 WINNER_OVER_BUDGET
        rfq.setCreatedAt(java.time.LocalDateTime.now());
        return rfq;
    }
}
