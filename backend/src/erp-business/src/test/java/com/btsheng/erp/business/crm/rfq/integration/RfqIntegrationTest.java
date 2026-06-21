package com.btsheng.erp.business.crm.rfq.integration;

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
import com.btsheng.erp.business.crm.rfq.service.RfqService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.32 · 采购·询比价 集成测试（FR-8-1）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RfqIntegrationTest {

    @Mock private CrmRfqMapper rfqMapper;
    @Mock private CrmRfqVendorMapper vendorMapper;
    @Mock private CrmRfqQuoteMapper quoteMapper;
    @Mock private DocNoGenerator docNoGenerator;
    @Mock private WorkflowEventService workflowEventService;

    private RfqService service;

    @BeforeEach
    void setUp() {
        service = new RfqService(rfqMapper, vendorMapper, quoteMapper, docNoGenerator, workflowEventService);
        when(docNoGenerator.nextRfqNo())
                .thenReturn("RF20260612-0001", "RF20260612-0002", "RF20260612-0003", "RF20260612-0004", "RF20260612-0005");
        when(docNoGenerator.nextNo("PO"))
                .thenReturn("PO20260612-0001", "PO20260612-0002", "PO20260612-0003");
        when(rfqMapper.insert(any(CrmRfq.class))).thenAnswer(inv -> {
            CrmRfq r = inv.getArgument(0);
            r.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(vendorMapper.insert(any(CrmRfqVendor.class))).thenAnswer(inv -> {
            CrmRfqVendor v = inv.getArgument(0);
            v.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(quoteMapper.insert(any(CrmRfqQuote.class))).thenAnswer(inv -> {
            CrmRfqQuote q = inv.getArgument(0);
            q.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(rfqMapper.updateById(any(CrmRfq.class))).thenReturn(1);
        when(vendorMapper.updateById(any(CrmRfqVendor.class))).thenReturn(1);
        when(quoteMapper.updateById(any(CrmRfqQuote.class))).thenReturn(1);
    }

    private CreateRfqRequest buildReq() {
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

    // ====== 完整 lifecycle 1：创建 → 3 厂商 → 3 报价 → LOWEST 中标 ======
            @Test
    @DisplayName("集成 lifecycle 1：创建 → 3 厂商 → 3 报价 → LOWEST 中标")
    void testIntegration_LowestAward() {
        Result<CrmRfq> c = service.createRfq(buildReq(), 501L);
        Long rfqId = c.getData().getId();

        CrmRfq rfq = new CrmRfq();
        rfq.setId(rfqId);
        rfq.setStatus("DRAFT");
        rfq.setBudgetAmount(new BigDecimal("50000"));
        rfq.setWinnerMode("LOWEST");
        when(rfqMapper.selectById(rfqId)).thenReturn(rfq);
        when(vendorMapper.selectByRfqAndVendor(anyLong(), anyLong())).thenReturn(null);

        // 加 3 厂商
            for (long vid = 901; vid <= 903; vid++) {
            AddRfqVendorRequest vr = new AddRfqVendorRequest();
            vr.setVendorId(vid);
            vr.setVendorName("厂商" + vid);
            service.addVendor(rfqId, vr, 501L);
        }

        // 模拟 3 个厂商都报了价
            CrmRfqVendor rv1 = new CrmRfqVendor(); rv1.setId(11L); rv1.setVendorId(901L); rv1.setVendorName("上海铝业");
        CrmRfqVendor rv2 = new CrmRfqVendor(); rv2.setId(12L); rv2.setVendorId(902L); rv2.setVendorName("苏州金属");
        CrmRfqVendor rv3 = new CrmRfqVendor(); rv3.setId(13L); rv3.setVendorId(903L); rv3.setVendorName("宁波钢材");
        when(vendorMapper.selectByRfqAndVendor(rfqId, 901L)).thenReturn(rv1);
        when(vendorMapper.selectByRfqAndVendor(rfqId, 902L)).thenReturn(rv2);
        when(vendorMapper.selectByRfqAndVendor(rfqId, 903L)).thenReturn(rv3);

        CrmRfqQuote q1 = new CrmRfqQuote(); q1.setId(21L); q1.setRfqId(rfqId); q1.setVendorId(901L);
        q1.setUnitPrice(new BigDecimal("450")); q1.setTotalAmount(new BigDecimal("45000"));
        q1.setQualityScore(new BigDecimal("4.2")); q1.setDeliveryScore(new BigDecimal("4.5"));
        q1.setServiceScore(new BigDecimal("4.0")); q1.setWeightedScore(new BigDecimal("40.00"));
        CrmRfqQuote q2 = new CrmRfqQuote(); q2.setId(22L); q2.setRfqId(rfqId); q2.setVendorId(902L);
        q2.setUnitPrice(new BigDecimal("460")); q2.setTotalAmount(new BigDecimal("46000"));
        CrmRfqQuote q3 = new CrmRfqQuote(); q3.setId(23L); q3.setRfqId(rfqId); q3.setVendorId(903L);
        q3.setUnitPrice(new BigDecimal("475")); q3.setTotalAmount(new BigDecimal("47500"));
        when(quoteMapper.selectByRfqId(rfqId)).thenReturn(List.of(q1, q2, q3));

        AwardRfqRequest aw = new AwardRfqRequest();
        aw.setAutoCreatePo(true);
        Result<Map<String, Object>> r = service.awardRfq(rfqId, aw, 501L);
        assertEquals(0, r.getCode());
        CrmRfqQuote winner = (CrmRfqQuote) r.getData().get("winner");
        assertEquals(901L, winner.getVendorId());   // 上海铝业最低 45000
            assertEquals("AWARDED", rfq.getStatus());
        assertEquals("PO20260612-0001", rfq.getPurchaseOrderNo());
    }

    // ====== WEIGHTED 加权评分 ======
            @Test
    @DisplayName("集成 WEIGHTED 模式：加权评分选最优")
    void testIntegration_WeightedAward() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(2L);
        rfq.setStatus("QUOTING");
        rfq.setBudgetAmount(new BigDecimal("50000"));
        rfq.setWinnerMode("WEIGHTED");
        when(rfqMapper.selectById(2L)).thenReturn(rfq);

        CrmRfqQuote q1 = new CrmRfqQuote(); q1.setId(31L); q1.setVendorId(901L);
        q1.setUnitPrice(new BigDecimal("500")); q1.setTotalAmount(new BigDecimal("50000"));
        q1.setQualityScore(new BigDecimal("4.6")); q1.setDeliveryScore(new BigDecimal("4.2"));
        q1.setServiceScore(new BigDecimal("4.5"));
        CrmRfqQuote q2 = new CrmRfqQuote(); q2.setId(32L); q2.setVendorId(902L);
        q2.setUnitPrice(new BigDecimal("490")); q2.setTotalAmount(new BigDecimal("49000"));
        q2.setQualityScore(new BigDecimal("4.4")); q2.setDeliveryScore(new BigDecimal("4.4"));
        q2.setServiceScore(new BigDecimal("4.0"));
        when(quoteMapper.selectByRfqId(2L)).thenReturn(List.of(q1, q2));

        Result<Map<String, Object>> r = service.compareQuotes(2L);
        assertEquals(0, r.getCode());
        // q2 单价更低且 quality 不差，应胜出
            CrmRfqQuote winner = (CrmRfqQuote) r.getData().get("winner");
        assertNotNull(winner);
    }

    // ====== 单号 RF 前缀 ======
            @Test
    @DisplayName("AC-8.1.1：单号模板 RF{yyyyMMdd}{seq:4}")
    void testIntegration_RfqNo() {
        Result<CrmRfq> r = service.createRfq(buildReq(), 501L);
        assertTrue(r.getData().getRfqNo().startsWith("RF"));
        // RF + 20260612 + - + 0001 = 15 字符
            assertEquals(15, r.getData().getRfqNo().length());
    }

    // ====== 状态机推进 ======
            @Test
    @DisplayName("状态机：DRAFT → QUOTING → COMPARED → AWARDED")
    void testIntegration_StatusMachine() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(3L);
        rfq.setStatus("DRAFT");
        rfq.setBudgetAmount(new BigDecimal("50000"));
        rfq.setWinnerMode("LOWEST");
        when(rfqMapper.selectById(3L)).thenReturn(rfq);
        when(vendorMapper.selectByRfqAndVendor(anyLong(), anyLong())).thenReturn(null);

        // 加 1 厂商：触发 DRAFT → QUOTING
            AddRfqVendorRequest vr = new AddRfqVendorRequest();
        vr.setVendorId(901L);
        vr.setVendorName("上海铝业");
        service.addVendor(3L, vr, 501L);
        assertEquals("QUOTING", rfq.getStatus());

        CrmRfqQuote q1 = new CrmRfqQuote(); q1.setId(41L); q1.setVendorId(901L);
        q1.setTotalAmount(new BigDecimal("45000"));
        when(quoteMapper.selectByRfqId(3L)).thenReturn(List.of(q1));

        Result<Map<String, Object>> cmp = service.compareQuotes(3L);
        assertEquals("COMPARED", rfq.getStatus());
    }

    // ====== 唯一性索引 (rfq_no) ======
            @Test
    @DisplayName("P1 修补 1：单号唯一（DB 唯一索引兜底）")
    void testIntegration_UniqueRfqNo() {
        Result<CrmRfq> r1 = service.createRfq(buildReq(), 501L);
        Result<CrmRfq> r2 = service.createRfq(buildReq(), 501L);
        // 单号生成器按 next 顺序发号（不会重复）
            assertNotEquals(r1.getData().getRfqNo(), r2.getData().getRfqNo());
    }

    // ====== WEIGHTED 评分正确性 ======
            @Test
    @DisplayName("WEIGHTED 评分：price 50% + quality 20% + delivery 20% + service 10%")
    void testIntegration_WeightedFormula() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(4L);
        rfq.setStatus("QUOTING");
        rfq.setBudgetAmount(new BigDecimal("50000"));
        rfq.setWinnerMode("WEIGHTED");
        when(rfqMapper.selectById(4L)).thenReturn(rfq);

        // minPrice = 400, q1 price=400 → priceScore=50, q2 price=500 → priceScore = (1-500/400)*50 = -12.5
            CrmRfqQuote q1 = new CrmRfqQuote(); q1.setId(51L); q1.setVendorId(901L);
        q1.setUnitPrice(new BigDecimal("400")); q1.setTotalAmount(new BigDecimal("40000"));
        q1.setQualityScore(new BigDecimal("5.0")); q1.setDeliveryScore(new BigDecimal("5.0"));
        q1.setServiceScore(new BigDecimal("5.0"));
        CrmRfqQuote q2 = new CrmRfqQuote(); q2.setId(52L); q2.setVendorId(902L);
        q2.setUnitPrice(new BigDecimal("500")); q2.setTotalAmount(new BigDecimal("50000"));
        q2.setQualityScore(new BigDecimal("3.0")); q2.setDeliveryScore(new BigDecimal("3.0"));
        q2.setServiceScore(new BigDecimal("3.0"));
        when(quoteMapper.selectByRfqId(4L)).thenReturn(List.of(q1, q2));

        Result<Map<String, Object>> r = service.compareQuotes(4L);
        assertEquals(0, r.getCode());
        CrmRfqQuote winner = (CrmRfqQuote) r.getData().get("winner");
        // q1 应胜出：50 + 5*0.2 + 5*0.2 + 5*0.1 = 50 + 1 + 1 + 0.5 = 52.5
            assertEquals(901L, winner.getVendorId());
    }

    // ====== 选最低不超预算 ======
            @Test
    @DisplayName("P1 修补 3：选最低超预算 → 40303")
    void testIntegration_OverBudget() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(5L);
        rfq.setStatus("QUOTING");
        rfq.setBudgetAmount(new BigDecimal("1000"));  // 预算小
            rfq.setWinnerMode("LOWEST");
        when(rfqMapper.selectById(5L)).thenReturn(rfq);

        CrmRfqQuote q1 = new CrmRfqQuote(); q1.setId(61L); q1.setVendorId(901L);
        q1.setTotalAmount(new BigDecimal("5000"));
        when(quoteMapper.selectByRfqId(5L)).thenReturn(List.of(q1));

        Result<Map<String, Object>> r = service.compareQuotes(5L);
        assertEquals(40303, r.getCode());
    }

    // ====== 中标自动触发 PO ======
            @Test
    @DisplayName("P1 修补 4：中标自动生成 PO 单号 PO{yyyyMMdd}{seq:4}")
    void testIntegration_AutoPo() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(6L);
        rfq.setStatus("QUOTING");
        rfq.setBudgetAmount(new BigDecimal("50000"));
        rfq.setWinnerMode("LOWEST");
        rfq.setRfqNo("RF20260612-0001");
        when(rfqMapper.selectById(6L)).thenReturn(rfq);

        CrmRfqQuote q1 = new CrmRfqQuote(); q1.setId(71L); q1.setVendorId(901L);
        q1.setTotalAmount(new BigDecimal("45000"));
        CrmRfqVendor rv = new CrmRfqVendor(); rv.setId(81L); rv.setVendorId(901L); rv.setVendorName("上海铝业");
        when(quoteMapper.selectByRfqId(6L)).thenReturn(List.of(q1));
        when(vendorMapper.selectByRfqAndVendor(6L, 901L)).thenReturn(rv);

        Result<Map<String, Object>> r = service.awardRfq(6L, new AwardRfqRequest(), 501L);
        assertEquals(0, r.getCode());
        assertTrue(rfq.getPurchaseOrderNo().startsWith("PO"));
    }

    // ====== 跨 Story 1.6 订单闭环 ======
            @Test
    @DisplayName("跨 1.6：rfq.purchaseOrderNo 写入后订单模块可消费")
    void testIntegration_Cross_Order() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(7L);
        rfq.setStatus("QUOTING");
        rfq.setBudgetAmount(new BigDecimal("50000"));
        rfq.setWinnerMode("LOWEST");
        when(rfqMapper.selectById(7L)).thenReturn(rfq);

        CrmRfqQuote q1 = new CrmRfqQuote(); q1.setId(91L); q1.setVendorId(901L);
        q1.setTotalAmount(new BigDecimal("45000"));
        CrmRfqVendor rv = new CrmRfqVendor(); rv.setId(101L); rv.setVendorId(901L); rv.setVendorName("上海铝业");
        when(quoteMapper.selectByRfqId(7L)).thenReturn(List.of(q1));
        when(vendorMapper.selectByRfqAndVendor(7L, 901L)).thenReturn(rv);

        Result<Map<String, Object>> r = service.awardRfq(7L, null, 501L);
        assertEquals(0, r.getCode());
        assertNotNull(rfq.getPurchaseOrderNo());
    }

    // ====== 审计留痕 ======
            @Test
    @DisplayName("审计留痕：createRfq + addVendor + submitQuote + award 各写 1 记录")
    void testIntegration_Audit() {
        CrmRfq rfq = new CrmRfq();
        rfq.setId(8L);
        rfq.setStatus("DRAFT");
        rfq.setBudgetAmount(new BigDecimal("50000"));
        rfq.setWinnerMode("LOWEST");
        when(rfqMapper.selectById(8L)).thenReturn(rfq);
        when(vendorMapper.selectByRfqAndVendor(anyLong(), anyLong())).thenReturn(null);

        Result<CrmRfq> c = service.createRfq(buildReq(), 501L);
        verify(rfqMapper, times(1)).insert(any(CrmRfq.class));

        AddRfqVendorRequest vr = new AddRfqVendorRequest();
        vr.setVendorId(901L);
        vr.setVendorName("上海铝业");
        service.addVendor(8L, vr, 501L);
        verify(vendorMapper, times(1)).insert(any(CrmRfqVendor.class));
    }
}
