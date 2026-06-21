package com.btsheng.erp.business.finance.receivable.service;

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
 * V1.3.7 · Story 1.36 · 财务·应收应付 Service 单元测试（FR-9-1）
 * 18 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReceivablePayableServiceTest {

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
                .thenReturn("RV20260612-0001", "RV20260612-0002", "RV20260612-0003");
        when(docNoGenerator.nextPayableNo())
                .thenReturn("PV20260612-0001", "PV20260612-0002", "PV20260612-0003");
        when(docNoGenerator.nextPaymentNo())
                .thenReturn("PM20260612-0001", "PM20260612-0002", "PM20260612-0003");
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

    // ====== createReceivable 5 测例 ======
            @Test
    @DisplayName("createReceivable happy path · 单号 RV 前缀")
    void testCreateRec_OK() {
        when(receivableMapper.selectByOrderId(any())).thenReturn(null);
        CreateReceivableRequest r = new CreateReceivableRequest();
        r.setCustomerId(301L); r.setCustomerName("上海汽车配件");
        r.setOrderId(5001L); r.setOrderNo("XS20260501-0001");
        r.setTotalAmount(new BigDecimal("120000"));
        r.setDueDate(LocalDate.now().plusDays(15));
        Result<CrmReceivable> res = service.createReceivable(r, 701L);
        assertEquals(0, res.getCode());
        assertTrue(res.getData().getReceivableNo().startsWith("RV"));
        assertEquals("OPEN", res.getData().getStatus());
    }

    @Test
    @DisplayName("P1 修补 1：金额为负 → 40001")
    void testCreateRec_Negative() {
        CreateReceivableRequest r = new CreateReceivableRequest();
        r.setOrderId(5001L); r.setOrderNo("XS1");
        r.setTotalAmount(new BigDecimal("-100"));
        r.setDueDate(LocalDate.now());
        Result<CrmReceivable> res = service.createReceivable(r, 701L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("订单 ID 重复 → 40902")
    void testCreateRec_Duplicate() {
        CrmReceivable existed = new CrmReceivable();
        existed.setId(99L);
        when(receivableMapper.selectByOrderId(5001L)).thenReturn(existed);
        CreateReceivableRequest r = new CreateReceivableRequest();
        r.setOrderId(5001L); r.setOrderNo("XS1");
        r.setTotalAmount(new BigDecimal("100"));
        r.setDueDate(LocalDate.now());
        Result<CrmReceivable> res = service.createReceivable(r, 701L);
        assertEquals(40902, res.getCode());
    }

    @Test
    @DisplayName("createReceivable 缺到期日 → 40001")
    void testCreateRec_NoDueDate() {
        CreateReceivableRequest r = new CreateReceivableRequest();
        r.setOrderId(5001L); r.setOrderNo("XS1");
        r.setTotalAmount(new BigDecimal("100"));
        Result<CrmReceivable> res = service.createReceivable(r, 701L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("createReceivable 缺订单 ID → 40001")
    void testCreateRec_NoOrderId() {
        CreateReceivableRequest r = new CreateReceivableRequest();
        r.setTotalAmount(new BigDecimal("100"));
        r.setDueDate(LocalDate.now());
        Result<CrmReceivable> res = service.createReceivable(r, 701L);
        assertEquals(40001, res.getCode());
    }

    // ====== createPayable 3 测例 ======
            @Test
    @DisplayName("createPayable happy path · 单号 PV 前缀")
    void testCreatePay_OK() {
        when(payableMapper.selectByPoId(any())).thenReturn(null);
        CreatePayableRequest p = new CreatePayableRequest();
        p.setVendorId(901L); p.setVendorName("上海铝业");
        p.setPoId(1001L); p.setPoNo("PO20260501-0001");
        p.setTotalAmount(new BigDecimal("60000"));
        p.setDueDate(LocalDate.now().plusDays(20));
        Result<CrmPayable> res = service.createPayable(p, 702L);
        assertEquals(0, res.getCode());
        assertTrue(res.getData().getPayableNo().startsWith("PV"));
    }

    @Test
    @DisplayName("P1 修补 1：应付金额为负 → 40001")
    void testCreatePay_Negative() {
        CreatePayableRequest p = new CreatePayableRequest();
        p.setPoId(1001L); p.setPoNo("PO1");
        p.setTotalAmount(new BigDecimal("-1"));
        p.setDueDate(LocalDate.now());
        Result<CrmPayable> res = service.createPayable(p, 702L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("createPayable PO 重复 → 40902")
    void testCreatePay_Duplicate() {
        CrmPayable existed = new CrmPayable();
        existed.setId(99L);
        when(payableMapper.selectByPoId(1001L)).thenReturn(existed);
        CreatePayableRequest p = new CreatePayableRequest();
        p.setPoId(1001L); p.setPoNo("PO1");
        p.setTotalAmount(new BigDecimal("100"));
        p.setDueDate(LocalDate.now());
        Result<CrmPayable> res = service.createPayable(p, 702L);
        assertEquals(40902, res.getCode());
    }

    // ====== recordPayment 6 测例 ======
            @Test
    @DisplayName("receipt 全额回款 → CLOSED")
    void testReceipt_Full() {
        CrmReceivable r = new CrmReceivable();
        r.setId(1L); r.setReceivableNo("RV1");
        r.setTotalAmount(new BigDecimal("120000"));
        r.setPaidAmount(BigDecimal.ZERO);
        r.setUnpaidAmount(new BigDecimal("120000"));
        r.setDueDate(LocalDate.now().plusDays(10));
        when(receivableMapper.selectById(1L)).thenReturn(r);

        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setType("RECEIPT"); req.setRefId(1L);
        req.setAmount(new BigDecimal("120000"));
        req.setPaidBy(701L);
        Result<Map<String, Object>> res = service.recordPayment(req, 701L);
        assertEquals(0, res.getCode());
        assertEquals("CLOSED", r.getStatus());
        assertEquals(0, new BigDecimal("0").compareTo(r.getUnpaidAmount()));
    }

    @Test
    @DisplayName("P1 修补 2：收款 > 未收 → 40909")
    void testReceipt_Exceed() {
        CrmReceivable r = new CrmReceivable();
        r.setId(1L); r.setReceivableNo("RV1");
        r.setTotalAmount(new BigDecimal("1000"));
        r.setPaidAmount(BigDecimal.ZERO);
        r.setUnpaidAmount(new BigDecimal("500"));
        when(receivableMapper.selectById(1L)).thenReturn(r);
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setType("RECEIPT"); req.setRefId(1L);
        req.setAmount(new BigDecimal("600"));
        Result<Map<String, Object>> res = service.recordPayment(req, 701L);
        assertEquals(40909, res.getCode());
        assertEquals("RECEIPT_EXCEED_UNPAID", res.getMessage());
    }

    @Test
    @DisplayName("receipt 部分回款 → PARTIAL")
    void testReceipt_Partial() {
        CrmReceivable r = new CrmReceivable();
        r.setId(1L); r.setReceivableNo("RV1");
        r.setTotalAmount(new BigDecimal("1000"));
        r.setPaidAmount(BigDecimal.ZERO);
        r.setUnpaidAmount(new BigDecimal("1000"));
        when(receivableMapper.selectById(1L)).thenReturn(r);
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setType("RECEIPT"); req.setRefId(1L);
        req.setAmount(new BigDecimal("300"));
        Result<Map<String, Object>> res = service.recordPayment(req, 701L);
        assertEquals(0, res.getCode());
        assertEquals("PARTIAL", r.getStatus());
        assertEquals(0, new BigDecimal("700").compareTo(r.getUnpaidAmount()));
    }

    @Test
    @DisplayName("payment 全额付款 → CLOSED")
    void testPayment_Full() {
        CrmPayable p = new CrmPayable();
        p.setId(1L); p.setPayableNo("PV1");
        p.setTotalAmount(new BigDecimal("50000"));
        p.setPaidAmount(BigDecimal.ZERO);
        p.setUnpaidAmount(new BigDecimal("50000"));
        when(payableMapper.selectById(1L)).thenReturn(p);
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setType("PAYMENT"); req.setRefId(1L);
        req.setAmount(new BigDecimal("50000"));
        Result<Map<String, Object>> res = service.recordPayment(req, 702L);
        assertEquals(0, res.getCode());
        assertEquals("CLOSED", p.getStatus());
    }

    @Test
    @DisplayName("P1 修补 2：付款 > 未付 → 40909")
    void testPayment_Exceed() {
        CrmPayable p = new CrmPayable();
        p.setId(1L); p.setPayableNo("PV1");
        p.setTotalAmount(new BigDecimal("1000"));
        p.setPaidAmount(BigDecimal.ZERO);
        p.setUnpaidAmount(new BigDecimal("300"));
        when(payableMapper.selectById(1L)).thenReturn(p);
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setType("PAYMENT"); req.setRefId(1L);
        req.setAmount(new BigDecimal("500"));
        Result<Map<String, Object>> res = service.recordPayment(req, 702L);
        assertEquals(40909, res.getCode());
        assertEquals("PAYMENT_EXCEED_UNPAID", res.getMessage());
    }

    @Test
    @DisplayName("recordPayment 类型非法 → 40001")
    void testPayment_BadType() {
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setType("FOO"); req.setRefId(1L);
        req.setAmount(new BigDecimal("100"));
        Result<Map<String, Object>> res = service.recordPayment(req, 701L);
        assertEquals(40001, res.getCode());
    }

    // ====== aging + pending 4 测例 ======
            @Test
    @DisplayName("P1 修补 3：账龄 4 段 · D30/D60/D90/CURRENT")
    void testAging_Buckets() {
        CrmReceivable r1 = new CrmReceivable();
        r1.setId(1L); r1.setUnpaidAmount(new BigDecimal("100"));
        r1.setDueDate(LocalDate.now().plusDays(10));
        CrmReceivable r2 = new CrmReceivable();
        r2.setId(2L); r2.setUnpaidAmount(new BigDecimal("200"));
        r2.setDueDate(LocalDate.now().minusDays(45));
        CrmReceivable r3 = new CrmReceivable();
        r3.setId(3L); r3.setUnpaidAmount(new BigDecimal("300"));
        r3.setDueDate(LocalDate.now().minusDays(75));
        CrmReceivable r4 = new CrmReceivable();
        r4.setId(4L); r4.setUnpaidAmount(new BigDecimal("400"));
        r4.setDueDate(LocalDate.now().minusDays(100));
        when(receivableMapper.selectAll()).thenReturn(List.of(r1, r2, r3, r4));
        when(payableMapper.selectAll()).thenReturn(new ArrayList<>());

        Result<Map<String, Object>> r = service.getAging();
        assertEquals(0, r.getCode());
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> recvBucket =
                (Map<String, BigDecimal>) r.getData().get("receivable_by_bucket");
        assertEquals(0, new BigDecimal("100").compareTo(recvBucket.get("CURRENT")));
        assertEquals(0, new BigDecimal("200").compareTo(recvBucket.get("D30")));
        assertEquals(0, new BigDecimal("300").compareTo(recvBucket.get("D60")));
        assertEquals(0, new BigDecimal("400").compareTo(recvBucket.get("D90")));
    }

    @Test
    @DisplayName("P1 修补 3：aging_days 计算")
    void testAging_DaysCalc() {
        CrmReceivable r = new CrmReceivable();
        r.setId(1L); r.setUnpaidAmount(new BigDecimal("100"));
        r.setDueDate(LocalDate.now().minusDays(75));
        when(receivableMapper.selectAll()).thenReturn(List.of(r));
        when(payableMapper.selectAll()).thenReturn(new ArrayList<>());
        service.getAging();
        assertEquals(75, r.getAgingDays());
        assertEquals("D60", r.getAgingBucket());
    }

    @Test
    @DisplayName("listPending · 未结清返回")
    void testListPending_OK() {
        CrmReceivable r = new CrmReceivable();
        r.setId(1L); r.setStatus("OPEN");
        r.setUnpaidAmount(new BigDecimal("100"));
        CrmPayable p = new CrmPayable();
        p.setId(1L); p.setStatus("OPEN");
        p.setUnpaidAmount(new BigDecimal("200"));
        when(receivableMapper.selectOpen()).thenReturn(List.of(r));
        when(payableMapper.selectOpen()).thenReturn(List.of(p));
        Result<Map<String, Object>> res = service.listPending();
        assertEquals(0, res.getCode());
        assertEquals(1, ((List<?>) res.getData().get("receivables")).size());
        assertEquals(1, ((List<?>) res.getData().get("payables")).size());
    }

    @Test
    @DisplayName("listPending 空列表")
    void testListPending_Empty() {
        when(receivableMapper.selectOpen()).thenReturn(new ArrayList<>());
        when(payableMapper.selectOpen()).thenReturn(new ArrayList<>());
        Result<Map<String, Object>> res = service.listPending();
        assertEquals(0, res.getCode());
    }
}
