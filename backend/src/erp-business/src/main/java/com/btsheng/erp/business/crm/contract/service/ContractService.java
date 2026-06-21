package com.btsheng.erp.business.crm.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btsheng.erp.business.crm.contract.entity.SalesContract;
import com.btsheng.erp.business.crm.contract.entity.SalesPaymentPlan;
import com.btsheng.erp.business.crm.contract.entity.SalesReceipt;
import com.btsheng.erp.business.crm.contract.mapper.SalesContractMapper;
import com.btsheng.erp.business.crm.contract.mapper.SalesPaymentPlanMapper;
import com.btsheng.erp.business.crm.contract.mapper.SalesReceiptMapper;
import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.crm.sales.entity.CrmCustomer;
import com.btsheng.erp.business.crm.sales.mapper.CrmCustomerMapper;
import com.btsheng.erp.business.finance.profit.service.ProfitAnalysisService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.SalesDataScopeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContractService {

    /** PRD AC-2.4 · 回款以销售订单为业务主体；草稿/取消订单不进入回款列表 */
    private static final Set<String> EXCLUDED_ORDER_STATUSES = Set.of("DRAFT", "CANCELLED");

    private final SalesContractMapper contractMapper;
    private final SalesPaymentPlanMapper planMapper;
    private final SalesReceiptMapper receiptMapper;
    private final CrmOrderMapper orderMapper;
    private final CrmCustomerMapper customerMapper;
    private final ProfitAnalysisService profitAnalysisService;

    @Autowired
    public ContractService(SalesContractMapper contractMapper,
                           SalesPaymentPlanMapper planMapper,
                           SalesReceiptMapper receiptMapper,
                           CrmOrderMapper orderMapper,
                           CrmCustomerMapper customerMapper,
                           ProfitAnalysisService profitAnalysisService) {
        this.contractMapper = contractMapper;
        this.planMapper = planMapper;
        this.receiptMapper = receiptMapper;
        this.orderMapper = orderMapper;
        this.customerMapper = customerMapper;
        this.profitAnalysisService = profitAnalysisService;
    }

    public Result<Map<String, Object>> list(String keyword, int pageNum, int pageSize) {
        Long scopedOwner = SalesDataScopeHelper.resolveOwnerUserId(null);
        SalesDataScopeHelper.Scope scope = SalesDataScopeHelper.effectiveScope();

        List<CrmOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<CrmOrder>()
                        .eq(CrmOrder::getIsDeleted, 0)
                        .notIn(CrmOrder::getStatus, EXCLUDED_ORDER_STATUSES)
                        .orderByDesc(CrmOrder::getId));

        List<Map<String, Object>> enriched = new ArrayList<>();
        for (CrmOrder order : orders) {
            if (scope == SalesDataScopeHelper.Scope.SELF && !isOwnedBy(order, scopedOwner)) {
                continue;
            }
            if (scope == SalesDataScopeHelper.Scope.DEPT) {
                Long deptId = SalesDataScopeHelper.resolveDeptId(null);
                if (deptId == null || !deptId.equals(order.getDeptId())) {
                    continue;
                }
            }
            CrmCustomer customer = loadCustomer(order.getCustomerId());
            if (keyword != null && !keyword.isBlank() && !matchesKeyword(keyword, order, customer)) {
                continue;
            }
            SalesContract contract = resolveOrCreateContract(order);
            enriched.add(toListRow(contract, order, customer));
        }

        int size = pageSize > 0 ? pageSize : 20;
        int page = Math.max(pageNum, 1);
        int from = (page - 1) * size;
        int to = Math.min(from + size, enriched.size());
        List<Map<String, Object>> items = from < enriched.size() ? enriched.subList(from, to) : List.of();

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("items", items);
        pageData.put("records", items);
        pageData.put("total", enriched.size());
        pageData.put("pageNum", page);
        pageData.put("pageSize", size);
        return Result.ok(pageData);
    }

    /** 合同回款详情（业务主体 = 关联销售订单） */
    public Result<Map<String, Object>> getContract(Long id) {
        SalesContract c = requireContract(id);
        if (c == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "CONTRACT_NOT_FOUND");
        }
        Result<Void> scope = assertContractScope(c);
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        CrmOrder order = c.getOrderId() != null ? orderMapper.selectById(c.getOrderId()) : null;
        if (order == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "ORDER_NOT_FOUND");
        }
        CrmCustomer customer = loadCustomer(order.getCustomerId());
        return Result.ok(toListRow(c, order, customer));
    }

    public Result<List<Map<String, Object>>> paymentPlan(Long contractId) {
        SalesContract c = requireContract(contractId);
        if (c == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "CONTRACT_NOT_FOUND");
        }
        Result<Void> scope = assertContractScope(c);
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        List<SalesPaymentPlan> plans = planMapper.selectList(
                new LambdaQueryWrapper<SalesPaymentPlan>()
                        .eq(SalesPaymentPlan::getContractId, contractId)
                        .orderByAsc(SalesPaymentPlan::getPeriodNo));
        List<Map<String, Object>> rows = plans.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("phase", p.getPeriodNo());
            m.put("planDate", p.getPlanDate());
            m.put("planAmount", p.getPlanAmount());
            m.put("actualAmount", p.getActualAmount());
            m.put("status", p.getStatus());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(rows);
    }

    public Result<Map<String, Object>> savePaymentPlan(Long contractId, Long planId, Map<String, Object> body) {
        SalesContract c = requireContract(contractId);
        if (c == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "CONTRACT_NOT_FOUND");
        }
        Result<Void> scope = assertContractScope(c);
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        Integer periodNo = parseInteger(body.get("phase"), body.get("periodNo"));
        LocalDate planDate = parseDate(body.get("planDate"));
        BigDecimal planAmount = parseDecimal(body.get("planAmount"));
        if (periodNo == null || planDate == null || planAmount == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "PLAN_FIELDS_REQUIRED");
        }
        SalesPaymentPlan plan;
        if (planId != null) {
            plan = planMapper.selectById(planId);
            if (plan == null || !contractId.equals(plan.getContractId())) {
                return Result.fail(Result.CODE_NOT_FOUND, "PAYMENT_PLAN_NOT_FOUND");
            }
        } else {
            plan = new SalesPaymentPlan();
            plan.setContractId(contractId);
            plan.setStatus("PLANNED");
        }
        plan.setPeriodNo(periodNo);
        plan.setPlanDate(planDate);
        plan.setPlanAmount(planAmount);
        if (body.get("status") != null) {
            plan.setStatus(String.valueOf(body.get("status")));
        } else if (plan.getStatus() == null) {
            plan.setStatus("PLANNED");
        }
        if (planId != null) {
            planMapper.updateById(plan);
        } else {
            planMapper.insert(plan);
        }
        Map<String, Object> row = new HashMap<>();
        row.put("id", plan.getId());
        row.put("phase", plan.getPeriodNo());
        row.put("planDate", plan.getPlanDate());
        row.put("planAmount", plan.getPlanAmount());
        row.put("status", plan.getStatus());
        return Result.ok(row);
    }

    public Result<Void> deletePaymentPlan(Long contractId, Long planId) {
        SalesContract c = requireContract(contractId);
        if (c == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "CONTRACT_NOT_FOUND");
        }
        Result<Void> scope = assertContractScope(c);
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        SalesPaymentPlan plan = planMapper.selectById(planId);
        if (plan == null || !contractId.equals(plan.getContractId())) {
            return Result.fail(Result.CODE_NOT_FOUND, "PAYMENT_PLAN_NOT_FOUND");
        }
        planMapper.deleteById(planId);
        return Result.ok(null);
    }

    public Result<List<Map<String, Object>>> paymentReg(Long contractId) {
        SalesContract c = requireContract(contractId);
        if (c == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "CONTRACT_NOT_FOUND");
        }
        Result<Void> scope = assertContractScope(c);
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        List<SalesReceipt> receipts = receiptMapper.selectList(
                new LambdaQueryWrapper<SalesReceipt>()
                        .eq(SalesReceipt::getContractId, contractId)
                        .orderByDesc(SalesReceipt::getReceiptDate));
        List<Map<String, Object>> rows = receipts.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("paidDate", r.getReceiptDate());
            m.put("paidAmount", r.getAmount());
            m.put("method", r.getPayer());
            m.put("remark", r.getRemark());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(rows);
    }

    public Result<Map<String, Object>> savePaymentReg(Long contractId, Long receiptId, Map<String, Object> body) {
        SalesContract c = requireContract(contractId);
        if (c == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "CONTRACT_NOT_FOUND");
        }
        Result<Void> scope = assertContractScope(c);
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        LocalDate receiptDate = parseDate(body.get("paidDate"), body.get("receiptDate"));
        BigDecimal amount = parseDecimal(body.get("paidAmount"), body.get("amount"));
        if (receiptDate == null || amount == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "RECEIPT_FIELDS_REQUIRED");
        }
        SalesReceipt receipt;
        if (receiptId != null) {
            receipt = receiptMapper.selectById(receiptId);
            if (receipt == null || !contractId.equals(receipt.getContractId())) {
                return Result.fail(Result.CODE_NOT_FOUND, "RECEIPT_NOT_FOUND");
            }
        } else {
            receipt = new SalesReceipt();
            receipt.setContractId(contractId);
        }
        receipt.setReceiptDate(receiptDate);
        receipt.setAmount(amount);
        if (body.get("method") != null) {
            receipt.setPayer(String.valueOf(body.get("method")));
        } else if (body.get("payer") != null) {
            receipt.setPayer(String.valueOf(body.get("payer")));
        }
        if (body.get("remark") != null) {
            receipt.setRemark(String.valueOf(body.get("remark")));
        }
        if (receiptId != null) {
            receiptMapper.updateById(receipt);
        } else {
            receiptMapper.insert(receipt);
        }
        syncContractReceiptStatus(c);
        Map<String, Object> row = new HashMap<>();
        row.put("id", receipt.getId());
        row.put("paidDate", receipt.getReceiptDate());
        row.put("paidAmount", receipt.getAmount());
        row.put("method", receipt.getPayer());
        row.put("remark", receipt.getRemark());
        return Result.ok(row);
    }

    public Result<Void> deletePaymentReg(Long contractId, Long receiptId) {
        SalesContract c = requireContract(contractId);
        if (c == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "CONTRACT_NOT_FOUND");
        }
        Result<Void> scope = assertContractScope(c);
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        SalesReceipt receipt = receiptMapper.selectById(receiptId);
        if (receipt == null || !contractId.equals(receipt.getContractId())) {
            return Result.fail(Result.CODE_NOT_FOUND, "RECEIPT_NOT_FOUND");
        }
        receiptMapper.deleteById(receiptId);
        syncContractReceiptStatus(c);
        return Result.ok(null);
    }

    public Result<List<Map<String, Object>>> profit(Long contractId) {
        SalesContract c = requireContract(contractId);
        if (c == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "CONTRACT_NOT_FOUND");
        }
        Result<Void> scope = assertContractScope(c);
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        if (c.getOrderId() == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "CONTRACT_ORDER_NOT_LINKED");
        }
        Result<Map<String, Object>> profitResult = profitAnalysisService.resolveOrderProfit(c.getOrderId());
        if (!profitResult.isSuccess()) {
            return Result.fail(profitResult.getCode(), profitResult.getMessage());
        }
        Map<String, Object> row = new HashMap<>();
        Map<String, Object> data = profitResult.getData();
        row.put("revenue", data.get("revenue"));
        row.put("cost", data.get("cost"));
        row.put("marginRate", data.get("marginRate"));
        row.put("profit", data.get("profit"));
        row.put("segments", data.get("segments"));
        row.put("receivedAmount", sumReceipts(contractId));
        CrmOrder order = orderMapper.selectById(c.getOrderId());
        if (order != null) {
            row.put("orderNo", order.getOrderNo());
            row.put("orderAmount", order.getTotalAmount());
            row.put("customerName", order.getCustomerName());
        }
        return Result.ok(List.of(row));
    }

    private SalesContract requireContract(Long id) {
        return contractMapper.selectById(id);
    }

    private Result<Void> assertContractScope(SalesContract c) {
        if (SalesDataScopeHelper.effectiveScope() == SalesDataScopeHelper.Scope.ALL) {
            return Result.ok(null);
        }
        CrmOrder order = c.getOrderId() != null ? orderMapper.selectById(c.getOrderId()) : null;
        if (order == null) {
            return Result.fail(40302, "DATA_SCOPE");
        }
        return SalesDataScopeHelper.assertOwnerDept(order.getOwnerUserId(), order.getDeptId());
    }

    private static boolean isOwnedBy(CrmOrder order, Long ownerUserId) {
        return order != null && ownerUserId != null && ownerUserId.equals(order.getOwnerUserId());
    }

    private BigDecimal sumReceipts(Long contractId) {
        List<SalesReceipt> receipts = receiptMapper.selectList(
                new LambdaQueryWrapper<SalesReceipt>().eq(SalesReceipt::getContractId, contractId));
        return receipts.stream()
                .map(SalesReceipt::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Object> toListRow(SalesContract c, CrmOrder order, CrmCustomer customer) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("orderId", order.getId());
        m.put("orderNo", order.getOrderNo());
        // PRD AC-2.4 · 合同号与订单号统一展示（XS+日期+流水）
        m.put("contractNo", order.getOrderNo());
        m.put("customerName", order.getCustomerName());
        m.put("contactName", customer != null ? customer.getContactName() : null);
        m.put("contactPhone", customer != null ? customer.getContactPhone() : null);
        m.put("amount", order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
        m.put("orderAmount", order.getTotalAmount());
        m.put("orderStatus", order.getStatus());
        m.put("receivedAmount", sumReceipts(c.getId()));
        m.put("status", c.getStatus());
        attachProfitSummary(m, order.getId());
        return m;
    }

    private void attachProfitSummary(Map<String, Object> row, Long orderId) {
        Result<Map<String, Object>> profitResult = profitAnalysisService.resolveOrderProfit(orderId);
        if (!profitResult.isSuccess() || profitResult.getData() == null) {
            return;
        }
        Map<String, Object> data = profitResult.getData();
        row.put("profit", data.get("profit"));
        row.put("marginRate", data.get("marginRate"));
        row.put("revenue", data.get("revenue"));
        row.put("cost", data.get("cost"));
    }

    private CrmCustomer loadCustomer(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return customerMapper.selectById(customerId);
    }

    private static boolean matchesKeyword(String keyword, CrmOrder order, CrmCustomer customer) {
        String kw = keyword.trim();
        if (order.getOrderNo() != null && order.getOrderNo().contains(kw)) {
            return true;
        }
        if (order.getCustomerName() != null && order.getCustomerName().contains(kw)) {
            return true;
        }
        if (customer != null) {
            if (customer.getContactName() != null && customer.getContactName().contains(kw)) {
                return true;
            }
            if (customer.getContactPhone() != null && customer.getContactPhone().contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /** 每个有效销售订单对应一条回款记录；contract_no 与 order_no 保持一致 */
    private SalesContract resolveOrCreateContract(CrmOrder order) {
        SalesContract existing = contractMapper.selectByOrderId(order.getId());
        if (existing != null) {
            if (order.getOrderNo() != null && !order.getOrderNo().equals(existing.getContractNo())) {
                existing.setContractNo(order.getOrderNo());
                contractMapper.updateById(existing);
            }
            return existing;
        }
        SalesContract created = new SalesContract();
        created.setOrderId(order.getId());
        created.setContractNo(order.getOrderNo());
        created.setSignedAt(order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : LocalDate.now());
        created.setStatus("PENDING");
        contractMapper.insert(created);
        return created;
    }

    /**
     * 财务在应收账款登记实收时，同步写入销售合同回款记录（供销售侧查看）。
     */
    @Transactional
    public void recordFinanceReceipt(Long orderId, LocalDate receiptDate, BigDecimal amount,
                                     String method, String remark) {
        if (orderId == null || receiptDate == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        CrmOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        SalesContract c = resolveOrCreateContract(order);
        SalesReceipt receipt = new SalesReceipt();
        receipt.setContractId(c.getId());
        receipt.setReceiptDate(receiptDate);
        receipt.setAmount(amount);
        receipt.setPayer(mapFinanceMethod(method));
        receipt.setRemark(remark);
        receiptMapper.insert(receipt);
        syncContractReceiptStatus(c);
    }

    private static String mapFinanceMethod(String method) {
        if (method == null) {
            return "银行转账";
        }
        return switch (method) {
            case "BANK" -> "银行转账";
            case "CASH" -> "现金";
            case "CHECK" -> "承兑汇票";
            case "WECHAT" -> "微信";
            case "ALIPAY" -> "支付宝";
            default -> method;
        };
    }

    private void syncContractReceiptStatus(SalesContract c) {
        CrmOrder order = c.getOrderId() != null ? orderMapper.selectById(c.getOrderId()) : null;
        BigDecimal total = order != null && order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal received = sumReceipts(c.getId());
        String status;
        if (received.compareTo(BigDecimal.ZERO) <= 0) {
            status = "PENDING";
        } else if (total.compareTo(BigDecimal.ZERO) > 0 && received.compareTo(total) >= 0) {
            status = "RECEIVED";
        } else {
            status = "PARTIAL";
        }
        c.setStatus(status);
        contractMapper.updateById(c);
    }

    private static Integer parseInteger(Object... values) {
        for (Object v : values) {
            if (v == null) continue;
            if (v instanceof Number n) return n.intValue();
            try {
                return Integer.parseInt(String.valueOf(v).trim());
            } catch (NumberFormatException ignored) {
                // try next
            }
        }
        return null;
    }

    private static LocalDate parseDate(Object... values) {
        for (Object v : values) {
            if (v == null) continue;
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) continue;
            try {
                return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    private static BigDecimal parseDecimal(Object... values) {
        for (Object v : values) {
            if (v == null) continue;
            try {
                return new BigDecimal(String.valueOf(v).trim());
            } catch (NumberFormatException ignored) {
                // try next
            }
        }
        return null;
    }
}
