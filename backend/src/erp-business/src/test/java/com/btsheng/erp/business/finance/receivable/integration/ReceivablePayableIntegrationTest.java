package com.btsheng.erp.business.finance.receivable.integration;

import com.btsheng.erp.business.crm.contract.service.ContractService;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.receivable.dto.CreatePayableRequest;
import com.btsheng.erp.business.finance.receivable.dto.CreateReceivableRequest;
import com.btsheng.erp.business.finance.receivable.dto.RecordPaymentRequest;
import com.btsheng.erp.business.finance.receivable.entity.CrmPayable;
import com.btsheng.erp.business.finance.receivable.entity.CrmPayment;
import com.btsheng.erp.business.finance.receivable.entity.CrmReceivable;
import com.btsheng.erp.business.finance.receivable.mapper.CrmPayableMapper;
import com.btsheng.erp.business.finance.receivable.mapper.CrmPaymentMapper;
import com.btsheng.erp.business.finance.receivable.mapper.CrmReceivableMapper;
import com.btsheng.erp.business.finance.receivable.service.ReceivablePayableService;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * V1.3.7 · Story 1.36 · 财务·应收应付 集成测试（FR-9-1）
 * 12 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReceivablePayableIntegrationTest {

    @Mock private CrmReceivableMapper receivableMapper;
    @Mock private CrmPayableMapper payableMapper;
    @Mock private CrmPaymentMapper paymentMapper;
    @Mock private DocNoGenerator docNoGenerator;
    @Mock private ContractService contractService;

    private ReceivablePayableService service;

    @BeforeEach
    void setUp() {
        service = new ReceivablePayableService(receivableMapper, payableMapper, paymentMapper, docNoGenerator, contractService);
        when(docNoGenerator.nextReceivableNo())
                .thenReturn("RV20260612-0001", "RV20260612-0002", "RV20260612-0003", "RV20260612-0004");
        when(docNoGenerator.nextPayableNo())
                .thenReturn("PV20260612-0001", "PV20260612-0002", "PV20260612-0003");
        when(docNoGenerator.nextPaymentNo())
                .thenReturn("PM20260612-0001", "PM20260612-0002", "PM20260612-0003", "PM20260612-0004");
        when(receivableMapper.insert(any(CrmReceivable.class))).thenAnswer(inv -> {
            CrmReceivable r = inv.getArgument(0);
            r.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(payableMapper.insert(any(CrmPayable.class))).thenAnswer(inv -> {
            CrmPayable p = inv.getArgument(0);
            p.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(paymentMapper.insert(any(CrmPayment.class))).thenAnswer(inv -> {
            CrmPayment p = inv.getArgument(0);
            p.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(receivableMapper.updateById(any(CrmReceivable.class))).thenReturn(1);
        when(payableMapper.updateById(any(CrmPayable.class))).thenReturn(1);
    }

    // ====== lifecycle 1：订单 → 应收 → 全额回款 ======
            @Test
    @DisplayName("lifecycle 1：订单 → 应收 → 全额回款 CLOSED")
    void testIntegration_FullReceipt() {
        when(receivableMapper.selectByOrderId(any())).thenReturn(null);
        CreateReceivableRequest cr = new CreateReceivableRequest();
        cr.setCustomerId(301L); cr.setOrderId(5001L); cr.setOrderNo("XS20260501-0001");
        cr.setTotalAmount(new BigDecimal("120000"));
        cr.setDueDate(LocalDate.now().plusDays(15));
        Result<CrmReceivable> c = service.createReceivable(cr, 701L);
        Long rid = c.getData().getId();

        when(receivableMapper.selectById(rid)).thenReturn(c.getData());
        RecordPaymentRequest pr = new RecordPaymentRequest();
        pr.setType("RECEIPT"); pr.setRefId(rid);
        pr.setAmount(new BigDecimal("120000"));
        pr.setPaidBy(701L);
        Result<Map<String, Object>> r = service.recordPayment(pr, 701L);
        assertEquals(0, r.getCode());
        assertEquals("CLOSED", c.getData().getStatus());
    }

    // ====== lifecycle 2：PO → 应付 → 部分付款 ======
            @Test
    @DisplayName("lifecycle 2：PO → 应付 → 部分付款 PARTIAL")
    void testIntegration_PartialPayment() {
        when(payableMapper.selectByPoId(any())).thenReturn(null);
        CreatePayableRequest cp = new CreatePayableRequest();
        cp.setVendorId(903L); cp.setPoId(1003L); cp.setPoNo("PO20260510-0003");
        cp.setTotalAmount(new BigDecimal("90000"));
        cp.setDueDate(LocalDate.now().minusDays(20));
        Result<CrmPayable> c = service.createPayable(cp, 702L);
        Long pid = c.getData().getId();
        when(payableMapper.selectById(pid)).thenReturn(c.getData());

        RecordPaymentRequest pr = new RecordPaymentRequest();
        pr.setType("PAYMENT"); pr.setRefId(pid);
        pr.setAmount(new BigDecimal("40000"));
        pr.setPaidBy(702L);
        Result<Map<String, Object>> r = service.recordPayment(pr, 702L);
        assertEquals(0, r.getCode());
        assertEquals("PARTIAL", c.getData().getStatus());
    }

    // ====== AC-9.1.1：单号模板 ======
            @Test
    @DisplayName("AC-9.1.1：单号模板 RV{yyyyMMdd}{seq:4}")
    void testIntegration_RvNo() {
        when(receivableMapper.selectByOrderId(any())).thenReturn(null);
        CreateReceivableRequest cr = new CreateReceivableRequest();
        cr.setCustomerId(311L); cr.setOrderId(5011L); cr.setOrderNo("XS11");
        cr.setTotalAmount(new BigDecimal("100"));
        cr.setDueDate(LocalDate.now().plusDays(5));
        Result<CrmReceivable> r = service.createReceivable(cr, 701L);
        assertTrue(r.getData().getReceivableNo().startsWith("RV"));
        assertEquals(15, r.getData().getReceivableNo().length());
    }

    @Test
    @DisplayName("AC-9.1.1：单号模板 PV{yyyyMMdd}{seq:4}")
    void testIntegration_PvNo() {
        when(payableMapper.selectByPoId(any())).thenReturn(null);
        CreatePayableRequest cp = new CreatePayableRequest();
        cp.setVendorId(911L); cp.setPoId(1011L); cp.setPoNo("PO11");
        cp.setTotalAmount(new BigDecimal("100"));
        cp.setDueDate(LocalDate.now().plusDays(5));
        Result<CrmPayable> r = service.createPayable(cp, 702L);
        assertTrue(r.getData().getPayableNo().startsWith("PV"));
    }

    // ====== AC-9.1.2：账龄 ======
            @Test
    @DisplayName("AC-9.1.2：账龄 4 段聚合")
    void testIntegration_Aging4Buckets() {
        List<CrmReceivable> rs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            CrmReceivable r = new CrmReceivable();
            r.setId((long) (i + 1));
            r.setUnpaidAmount(new BigDecimal("1000"));
            r.setDueDate(LocalDate.now().minusDays(i * 35L));
            rs.add(r);
        }
        when(receivableMapper.selectAll()).thenReturn(rs);
        when(payableMapper.selectAll()).thenReturn(new ArrayList<>());

        Result<Map<String, Object>> r = service.getAging();
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> bucket = (Map<String, BigDecimal>) r.getData().get("receivable_by_bucket");
        // 0 35 70 105 -> D30 D60 D90 D90
            assertTrue(bucket.get("D30").compareTo(BigDecimal.ZERO) > 0);
        assertTrue(bucket.get("D60").compareTo(BigDecimal.ZERO) > 0);
        assertTrue(bucket.get("D90").compareTo(BigDecimal.ZERO) > 0);
    }

    // ====== 跨订单/PO 关联 ======
            @Test
    @DisplayName("跨 1.6 订单关联：orderId + orderNo 双绑")
    void testIntegration_CrossOrder() {
        when(receivableMapper.selectByOrderId(5005L)).thenReturn(null);
        CreateReceivableRequest cr = new CreateReceivableRequest();
        cr.setCustomerId(305L); cr.setCustomerName("杭州仪器");
        cr.setOrderId(5005L); cr.setOrderNo("XS20260515-0005");
        cr.setTotalAmount(new BigDecimal("65000"));
        cr.setDueDate(LocalDate.now().minusDays(95));
        Result<CrmReceivable> r = service.createReceivable(cr, 701L);
        assertEquals("XS20260515-0005", r.getData().getOrderNo());
    }

    @Test
    @DisplayName("跨 1.32 PO 关联：poId + poNo 双绑")
    void testIntegration_CrossPO() {
        when(payableMapper.selectByPoId(1005L)).thenReturn(null);
        CreatePayableRequest cp = new CreatePayableRequest();
        cp.setVendorId(905L); cp.setVendorName("河北法兰");
        cp.setPoId(1005L); cp.setPoNo("PO20260515-0005");
        cp.setTotalAmount(new BigDecimal("35000"));
        cp.setDueDate(LocalDate.now().minusDays(100));
        Result<CrmPayable> r = service.createPayable(cp, 702L);
        assertEquals("PO20260515-0005", r.getData().getPoNo());
    }

    // ====== 逾期应收状态联动 ======
            @Test
    @DisplayName("逾期 95 天 → agingBucket D90")
    void testIntegration_OverdueStatus() {
        CrmReceivable r = new CrmReceivable();
        r.setId(1L); r.setUnpaidAmount(new BigDecimal("65000"));
        r.setDueDate(LocalDate.now().minusDays(95));
        when(receivableMapper.selectAll()).thenReturn(List.of(r));
        when(payableMapper.selectAll()).thenReturn(new ArrayList<>());
        service.getAging();
        assertEquals("D90", r.getAgingBucket());
    }

    // ====== 二次回款幂等 ======
            @Test
    @DisplayName("二次回款：PARTIAL → CLOSED")
    void testIntegration_SecondReceipt() {
        CrmReceivable r = new CrmReceivable();
        r.setId(1L); r.setReceivableNo("RV1");
        r.setTotalAmount(new BigDecimal("1000"));
        r.setPaidAmount(new BigDecimal("400"));
        r.setUnpaidAmount(new BigDecimal("600"));
        r.setStatus("PARTIAL");
        when(receivableMapper.selectById(1L)).thenReturn(r);
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setType("RECEIPT"); req.setRefId(1L);
        req.setAmount(new BigDecimal("600"));
        req.setPaidBy(701L);
        service.recordPayment(req, 701L);
        assertEquals("CLOSED", r.getStatus());
    }

    // ====== payment_no 模板 ======
            @Test
    @DisplayName("payment_no 模板 PM{yyyyMMdd}{seq:4}")
    void testIntegration_PmNo() {
        CrmReceivable r = new CrmReceivable();
        r.setId(1L); r.setReceivableNo("RV1");
        r.setTotalAmount(new BigDecimal("100"));
        r.setPaidAmount(BigDecimal.ZERO);
        r.setUnpaidAmount(new BigDecimal("100"));
        when(receivableMapper.selectById(1L)).thenReturn(r);
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setType("RECEIPT"); req.setRefId(1L);
        req.setAmount(new BigDecimal("50"));
        req.setPaidBy(701L);
        Result<Map<String, Object>> res = service.recordPayment(req, 701L);
        CrmPayment pm = (CrmPayment) res.getData().get("payment");
        assertTrue(pm.getPaymentNo().startsWith("PM"));
    }

    // ====== pending 汇总 ======
            @Test
    @DisplayName("pending · receivables + payables 汇总")
    void testIntegration_PendingSummary() {
        when(receivableMapper.selectOpen()).thenReturn(List.of(
                buildRecv(1, "OPEN", new BigDecimal("1000")),
                buildRecv(2, "PARTIAL", new BigDecimal("500"))
        ));
        when(payableMapper.selectOpen()).thenReturn(List.of(
                buildPay(11, "OPEN", new BigDecimal("2000"))
        ));
        Result<Map<String, Object>> r = service.listPending();
        assertEquals(0, r.getCode());
        assertEquals(2, ((List<?>) r.getData().get("receivables")).size());
        assertEquals(1, ((List<?>) r.getData().get("payables")).size());
    }

    // ====== 完整账龄 + 应收应付聚合 ======
            @Test
    @DisplayName("完整 aging 报表：recv_total + pay_total")
    void testIntegration_AgingTotal() {
        CrmReceivable r1 = new CrmReceivable();
        r1.setId(1L); r1.setUnpaidAmount(new BigDecimal("100"));
        r1.setDueDate(LocalDate.now().minusDays(50));
        CrmPayable p1 = new CrmPayable();
        p1.setId(1L); p1.setUnpaidAmount(new BigDecimal("200"));
        p1.setDueDate(LocalDate.now().minusDays(70));
        when(receivableMapper.selectAll()).thenReturn(List.of(r1));
        when(payableMapper.selectAll()).thenReturn(List.of(p1));

        Result<Map<String, Object>> r = service.getAging();
        assertEquals(0, new BigDecimal("100").compareTo((BigDecimal) r.getData().get("receivable_total")));
        assertEquals(0, new BigDecimal("200").compareTo((BigDecimal) r.getData().get("payable_total")));
    }

    private CrmReceivable buildRecv(long id, String status, BigDecimal unpaid) {
        CrmReceivable r = new CrmReceivable();
        r.setId(id); r.setStatus(status);
        r.setUnpaidAmount(unpaid);
        r.setTotalAmount(unpaid);
        r.setDueDate(LocalDate.now());
        return r;
    }

    private CrmPayable buildPay(long id, String status, BigDecimal unpaid) {
        CrmPayable p = new CrmPayable();
        p.setId(id); p.setStatus(status);
        p.setUnpaidAmount(unpaid);
        p.setTotalAmount(unpaid);
        p.setDueDate(LocalDate.now());
        return p;
    }
}
