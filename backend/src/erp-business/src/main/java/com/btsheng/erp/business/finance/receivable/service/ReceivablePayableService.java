package com.btsheng.erp.business.finance.receivable.service;

import com.btsheng.erp.business.crm.contract.service.ContractService;
import com.btsheng.erp.business.finance.receivable.dto.CreatePayableRequest;
import com.btsheng.erp.business.finance.receivable.dto.CreateReceivableRequest;
import com.btsheng.erp.business.finance.receivable.dto.RecordPaymentRequest;
import com.btsheng.erp.business.finance.receivable.entity.CrmPayable;
import com.btsheng.erp.business.finance.receivable.entity.CrmPayment;
import com.btsheng.erp.business.finance.receivable.entity.CrmReceivable;
import com.btsheng.erp.business.finance.receivable.mapper.CrmPayableMapper;
import com.btsheng.erp.business.finance.receivable.mapper.CrmPaymentMapper;
import com.btsheng.erp.business.finance.receivable.mapper.CrmReceivableMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.36 · 财务·应收应付 Service (FR-9-1)
 *
 * <p>5 业务方法：createReceivable / createPayable / recordPayment / getAging / listPending
 * <p>4 P1 修补：金额非�?/ 收付�?�?未收/未付 / 账龄 4 段（30/60/90/90+�? 跨订�?PO 关联
 */
@Service
public class ReceivablePayableService {

    public static final String TYPE_RECEIPT = "RECEIPT";
    public static final String TYPE_PAYMENT = "PAYMENT";

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_OVERDUE = "OVERDUE";

    public static final String BUCKET_CURRENT = "CURRENT";  // 0-30
            public static final String BUCKET_D30 = "D30";          // 30-60
    public static final String BUCKET_D60 = "D60";          // 60-90
            public static final String BUCKET_D90 = "D90";          // 90+

    private final CrmReceivableMapper receivableMapper;
    private final CrmPayableMapper payableMapper;
    private final CrmPaymentMapper paymentMapper;
    private final DocNoGenerator docNoGenerator;
    private final ContractService contractService;

    @Autowired
    public ReceivablePayableService(CrmReceivableMapper receivableMapper,
                                    CrmPayableMapper payableMapper,
                                    CrmPaymentMapper paymentMapper,
                                    DocNoGenerator docNoGenerator,
                                    ContractService contractService) {
        this.receivableMapper = receivableMapper;
        this.payableMapper = payableMapper;
        this.paymentMapper = paymentMapper;
        this.docNoGenerator = docNoGenerator;
        this.contractService = contractService;
    }

    /**
     * P1 修补 3：账龄段 4 段（边界值含上界�?-30 / 31-60 / 61-90 / 91+�?     */
    private String calcAgingBucket(int days) {
        if (days <= 30) return BUCKET_CURRENT;
        if (days <= 60) return BUCKET_D30;
        if (days <= 90) return BUCKET_D60;
        return BUCKET_D90;
    }

    /**
     * 实际账龄分档（用于业务断言：aging 60 �?�?D60�?     * �?calcAgingBucket 一致；提供大于 60 �?D60 业务语义�?     */
    private String calcAgingBucketBiz(int days) {
        if (days < 31) return BUCKET_CURRENT;
        if (days < 61) return BUCKET_D30;
        if (days < 91) return BUCKET_D60;
        return BUCKET_D90;
    }

    private void refreshAging(CrmReceivable r) {
        if (r.getDueDate() == null) return;
        long days = ChronoUnit.DAYS.between(r.getDueDate(), LocalDate.now());
        if (days < 0) days = 0;  // 未到期：aging = 0
            r.setAgingDays((int) days);
        r.setAgingBucket(calcAgingBucketBiz((int) days));
    }

    private void refreshAging(CrmPayable p) {
        if (p.getDueDate() == null) return;
        long days = ChronoUnit.DAYS.between(p.getDueDate(), LocalDate.now());
        if (days < 0) days = 0;
        p.setAgingDays((int) days);
        p.setAgingBucket(calcAgingBucketBiz((int) days));
    }

    /**
     * AC-9.1.1：销售订�?SETTLED 触发应收（无人工录入�?     * P1 修补 1：金额非�?     * 跨订单关�?     */
    @Transactional
    @AuditLog(module = "receivable_payable", action = "receivable.create")
    public Result<CrmReceivable> createReceivable(CreateReceivableRequest req, Long operatorUserId) {
        if (req == null || req.getOrderId() == null) return Result.fail(40001, "ORDER_ID_REQUIRED");
        if (req.getTotalAmount() == null || req.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail(40001, "AMOUNT_NEGATIVE");
        }
        if (req.getDueDate() == null) return Result.fail(40001, "DUE_DATE_REQUIRED");
        if (receivableMapper.selectByOrderId(req.getOrderId()) != null) {
            return Result.fail(40902, "RECEIVABLE_DUPLICATE");
        }
        CrmReceivable r = new CrmReceivable();
        r.setReceivableNo(docNoGenerator.nextReceivableNo());
        r.setCustomerId(req.getCustomerId());
        r.setCustomerName(req.getCustomerName());
        r.setOrderId(req.getOrderId());
        r.setOrderNo(req.getOrderNo());
        r.setTotalAmount(req.getTotalAmount());
        r.setPaidAmount(BigDecimal.ZERO);
        r.setUnpaidAmount(req.getTotalAmount());
        r.setDueDate(req.getDueDate());
        r.setStatus(STATUS_OPEN);
        r.setAgingDays(0);
        r.setAgingBucket(BUCKET_CURRENT);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        receivableMapper.insert(r);
        return Result.ok(r);
    }

    /**
     * AC-9.1.1：PO 触发应付
     */
    @Transactional
    @AuditLog(module = "receivable_payable", action = "payable.create")
    public Result<CrmPayable> createPayable(CreatePayableRequest req, Long operatorUserId) {
        if (req == null || req.getPoId() == null) return Result.fail(40001, "PO_ID_REQUIRED");
        if (req.getTotalAmount() == null || req.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail(40001, "AMOUNT_NEGATIVE");
        }
        if (req.getDueDate() == null) return Result.fail(40001, "DUE_DATE_REQUIRED");
        if (payableMapper.selectByPoId(req.getPoId()) != null) {
            return Result.fail(40902, "PAYABLE_DUPLICATE");
        }
        CrmPayable p = new CrmPayable();
        p.setPayableNo(docNoGenerator.nextPayableNo());
        p.setVendorId(req.getVendorId());
        p.setVendorName(req.getVendorName());
        p.setPoId(req.getPoId());
        p.setPoNo(req.getPoNo());
        p.setTotalAmount(req.getTotalAmount());
        p.setPaidAmount(BigDecimal.ZERO);
        p.setUnpaidAmount(req.getTotalAmount());
        p.setDueDate(req.getDueDate());
        p.setStatus(STATUS_OPEN);
        p.setAgingDays(0);
        p.setAgingBucket(BUCKET_CURRENT);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        payableMapper.insert(p);
        return Result.ok(p);
    }

    /**
     * AC-9.1.2：收/付款
     * P1 修补 2：收/付金�?�?未收/未付金额
     */
    @Transactional
    @AuditLog(module = "receivable_payable", action = "payment.record")
    public Result<Map<String, Object>> recordPayment(RecordPaymentRequest req, Long operatorUserId) {
        if (req == null || req.getType() == null) return Result.fail(40001, "TYPE_REQUIRED");
        if (req.getRefId() == null) return Result.fail(40001, "REF_ID_REQUIRED");
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "AMOUNT_INVALID");
        }

        String type = req.getType();
        if (!TYPE_RECEIPT.equals(type) && !TYPE_PAYMENT.equals(type)) {
            return Result.fail(40001, "TYPE_INVALID");
        }

        Map<String, Object> data = new HashMap<>();
        CrmPayment pm = new CrmPayment();
        pm.setPaymentNo(docNoGenerator.nextPaymentNo());
        pm.setType(type);
        pm.setAmount(req.getAmount());
        pm.setMethod(req.getMethod() == null ? "BANK" : req.getMethod());
        pm.setPaidBy(req.getPaidBy());
        pm.setPaidAt(parsePaidAt(req.getPaidDate()));
        pm.setRemark(req.getRemark());
        pm.setCreatedAt(LocalDateTime.now());

        if (TYPE_RECEIPT.equals(type)) {
            CrmReceivable r = receivableMapper.selectById(req.getRefId());
            if (r == null) return Result.fail(40404, "RECEIVABLE_NOT_FOUND");
            // P1 修补 2：收 �?未收
            if (req.getAmount().compareTo(r.getUnpaidAmount()) > 0) {
                return Result.fail(40909, "RECEIPT_EXCEED_UNPAID");
            }
            BigDecimal newPaid = r.getPaidAmount().add(req.getAmount());
            r.setPaidAmount(newPaid);
            r.setUnpaidAmount(r.getTotalAmount().subtract(newPaid));
            if (r.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
                r.setStatus(STATUS_CLOSED);
            } else {
                r.setStatus(STATUS_PARTIAL);
            }
            r.setUpdatedAt(LocalDateTime.now());
            refreshAging(r);
            receivableMapper.updateById(r);
            pm.setRefId(r.getId());
            pm.setRefNo(r.getReceivableNo());
            data.put("receivable", r);
            if (r.getOrderId() != null) {
                contractService.recordFinanceReceipt(
                        r.getOrderId(),
                        pm.getPaidAt().toLocalDate(),
                        req.getAmount(),
                        pm.getMethod(),
                        req.getRemark());
            }
        } else {
            CrmPayable p = payableMapper.selectById(req.getRefId());
            if (p == null) return Result.fail(40404, "PAYABLE_NOT_FOUND");
            // P1 修补 2：付 �?未付
            if (req.getAmount().compareTo(p.getUnpaidAmount()) > 0) {
                return Result.fail(40909, "PAYMENT_EXCEED_UNPAID");
            }
            BigDecimal newPaid = p.getPaidAmount().add(req.getAmount());
            p.setPaidAmount(newPaid);
            p.setUnpaidAmount(p.getTotalAmount().subtract(newPaid));
            if (p.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
                p.setStatus(STATUS_CLOSED);
            } else {
                p.setStatus(STATUS_PARTIAL);
            }
            p.setUpdatedAt(LocalDateTime.now());
            refreshAging(p);
            payableMapper.updateById(p);
            pm.setRefId(p.getId());
            pm.setRefNo(p.getPayableNo());
            data.put("payable", p);
        }
        paymentMapper.insert(pm);
        data.put("payment", pm);
        return Result.ok(data);
    }

    /**
     * AC-9.1.2：账龄分析（4 段：30/60/90/90+�?     */
    @AuditLog(module = "receivable_payable", action = "aging.report")
    public Result<Map<String, Object>> getAging() {
        Map<String, Object> out = new HashMap<>();
        // refresh
            List<CrmReceivable> rs = receivableMapper.selectAll();
        List<CrmPayable> ps = payableMapper.selectAll();
        BigDecimal recvTotal = BigDecimal.ZERO;
        BigDecimal payTotal = BigDecimal.ZERO;
        Map<String, BigDecimal> recvBucket = new HashMap<>();
        Map<String, BigDecimal> payBucket = new HashMap<>();
        for (String b : new String[]{BUCKET_CURRENT, BUCKET_D30, BUCKET_D60, BUCKET_D90}) {
            recvBucket.put(b, BigDecimal.ZERO);
            payBucket.put(b, BigDecimal.ZERO);
        }
        for (CrmReceivable r : rs) {
            refreshAging(r);
            receivableMapper.updateById(r);
            recvTotal = recvTotal.add(r.getUnpaidAmount());
            recvBucket.merge(r.getAgingBucket(), r.getUnpaidAmount(), BigDecimal::add);
        }
        for (CrmPayable p : ps) {
            refreshAging(p);
            payableMapper.updateById(p);
            payTotal = payTotal.add(p.getUnpaidAmount());
            payBucket.merge(p.getAgingBucket(), p.getUnpaidAmount(), BigDecimal::add);
        }
        out.put("receivable_total", recvTotal);
        out.put("payable_total", payTotal);
        out.put("receivable_by_bucket", recvBucket);
        out.put("payable_by_bucket", payBucket);
        return Result.ok(out);
    }

    /**
     * 待办：未结清应收 + 应付
     */
    @AuditLog(module = "receivable_payable", action = "list_pending")
    public Result<Map<String, Object>> listPending() {
        Map<String, Object> out = new HashMap<>();
        List<CrmReceivable> rs = receivableMapper.selectOpen();
        List<CrmPayable> ps = payableMapper.selectOpen();
        for (CrmReceivable r : rs) refreshAging(r);
        for (CrmPayable p : ps) refreshAging(p);
        out.put("receivables", rs);
        out.put("payables", ps);
        return Result.ok(out);
    }

    public Result<Map<String, Object>> listReceivables(String customer, int pageNum, int pageSize) {
        try {
            List<CrmReceivable> all = receivableMapper.selectAll();
            List<Map<String, Object>> items = new ArrayList<>();
            for (CrmReceivable r : all) {
                if (customer != null && !customer.isBlank()) {
                    String name = r.getCustomerName() == null ? "" : r.getCustomerName();
                    if (!name.contains(customer.trim())) {
                        continue;
                    }
                }
                refreshAging(r);
                items.add(toReceivableVo(r));
            }
            return Result.ok(pageSlice(items, pageNum, pageSize));
        } catch (Exception ex) {
            return Result.ok(emptyPage(pageNum, pageSize));
        }
    }

    public Result<Map<String, Object>> getReceivableDetail(Long id) {
        CrmReceivable r = receivableMapper.selectById(id);
        if (r == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "应收单不存在");
        }
        refreshAging(r);
        Map<String, Object> vo = toReceivableVo(r);
        List<Map<String, Object>> receipts = new ArrayList<>();
        for (CrmPayment pm : paymentMapper.selectReceiptsByRefId(id)) {
            Map<String, Object> row = new HashMap<>();
            row.put("receiptNo", pm.getPaymentNo());
            row.put("amount", pm.getAmount());
            row.put("receivedAt", pm.getPaidAt());
            row.put("method", formatPaymentMethod(pm.getMethod()));
            row.put("remark", pm.getRemark());
            receipts.add(row);
        }
        vo.put("receipts", receipts);
        vo.put("paidAmount", r.getPaidAmount());
        vo.put("orderId", r.getOrderId());
        vo.put("orderNo", r.getOrderNo());
        return Result.ok(vo);
    }

    public Result<Map<String, Object>> listPayables(String vendor, int pageNum, int pageSize) {
        List<CrmPayable> all = payableMapper.selectAll();
        List<Map<String, Object>> items = new ArrayList<>();
        for (CrmPayable p : all) {
            if (vendor != null && !vendor.isBlank()) {
                String name = p.getVendorName() == null ? "" : p.getVendorName();
                if (!name.contains(vendor.trim())) {
                    continue;
                }
            }
            refreshAging(p);
            items.add(toPayableVo(p));
        }
        return Result.ok(pageSlice(items, pageNum, pageSize));
    }

    public Result<Map<String, Object>> getPayableDetail(Long id) {
        CrmPayable p = payableMapper.selectById(id);
        if (p == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "应付单不存在");
        }
        refreshAging(p);
        return Result.ok(toPayableVo(p));
    }

    private Map<String, Object> pageSlice(List<Map<String, Object>> all, int pageNum, int pageSize) {
        int size = pageSize > 0 ? pageSize : 20;
        int page = Math.max(pageNum, 1);
        int from = (page - 1) * size;
        int to = Math.min(from + size, all.size());
        List<Map<String, Object>> slice = from < all.size() ? all.subList(from, to) : List.of();
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("items", slice);
        pageData.put("records", slice);
        pageData.put("list", slice);
        pageData.put("total", all.size());
        pageData.put("pageNum", page);
        pageData.put("pageSize", size);
        return pageData;
    }

    private Map<String, Object> emptyPage(int pageNum, int pageSize) {
        return pageSlice(List.of(), pageNum, pageSize);
    }

    private Map<String, Object> toReceivableVo(CrmReceivable r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("receivableNo", r.getReceivableNo());
        m.put("customerName", r.getCustomerName());
        m.put("contractNo", r.getOrderNo());
        m.put("orderNo", r.getOrderNo());
        m.put("totalAmount", r.getTotalAmount());
        m.put("paidAmount", r.getPaidAmount());
        m.put("unpaidAmount", r.getUnpaidAmount());
        m.put("amount", r.getUnpaidAmount() != null ? r.getUnpaidAmount() : r.getTotalAmount());
        m.put("dueDate", r.getDueDate());
        m.put("status", r.getStatus());
        return m;
    }

    private static LocalDateTime parsePaidAt(String paidDate) {
        if (paidDate != null && !paidDate.isBlank()) {
            try {
                return LocalDate.parse(paidDate.trim().length() > 10 ? paidDate.trim().substring(0, 10) : paidDate.trim())
                        .atStartOfDay();
            } catch (Exception ignored) {
                // fall through
            }
        }
        return LocalDateTime.now();
    }

    private static String formatPaymentMethod(String method) {
        if (method == null) return "银行转账";
        return switch (method) {
            case "BANK" -> "银行转账";
            case "CASH" -> "现金";
            case "CHECK" -> "承兑汇票";
            case "WECHAT" -> "微信";
            case "ALIPAY" -> "支付宝";
            default -> method;
        };
    }

    private Map<String, Object> toPayableVo(CrmPayable p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("payableNo", p.getPayableNo());
        m.put("vendorName", p.getVendorName());
        m.put("poNo", p.getPoNo());
        m.put("amount", p.getUnpaidAmount() != null ? p.getUnpaidAmount() : p.getTotalAmount());
        m.put("totalAmount", p.getTotalAmount());
        m.put("unpaidAmount", p.getUnpaidAmount());
        m.put("dueDate", p.getDueDate());
        m.put("status", p.getStatus());
        return m;
    }
}
