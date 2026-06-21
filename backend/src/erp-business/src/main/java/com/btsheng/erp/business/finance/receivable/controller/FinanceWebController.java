package com.btsheng.erp.business.finance.receivable.controller;

import com.btsheng.erp.business.finance.cost.entity.CrmCostAccounting;
import com.btsheng.erp.business.finance.cost.entity.CrmCostSegment;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostAccountingMapper;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostSegmentMapper;
import com.btsheng.erp.business.finance.cost.service.CostAccountingService;
import com.btsheng.erp.business.finance.payment.entity.CrmPaymentPlan;
import com.btsheng.erp.business.finance.payment.mapper.CrmPaymentPlanMapper;
import com.btsheng.erp.business.finance.payment.service.PaymentCollectionService;
import com.btsheng.erp.business.finance.profit.entity.CrmProfitAnalysis;
import com.btsheng.erp.business.finance.profit.mapper.CrmProfitAnalysisMapper;
import com.btsheng.erp.business.finance.profit.service.ProfitAnalysisService;
import com.btsheng.erp.business.finance.cost.dto.AggregateCostRequest;
import com.btsheng.erp.business.finance.payment.dto.CreatePlanRequest;
import com.btsheng.erp.business.finance.payment.dto.MarkPaidRequest;
import com.btsheng.erp.business.finance.receivable.dto.CreateReceivableRequest;
import com.btsheng.erp.business.finance.receivable.dto.RecordPaymentRequest;
import com.btsheng.erp.business.finance.receivable.entity.CrmReceivable;
import com.btsheng.erp.business.finance.receivable.service.ReceivablePayableService;
import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.CurrentUserHelper;
import com.btsheng.erp.core.web.SalesDataScopeHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Web 财务模块路径别名（/finance/*） */
@Tag(name = "E9-Finance-Web", description = "财务 Web 端点")
@RestController
@RequestMapping("/finance")
public class FinanceWebController {

    private final ReceivablePayableService receivableService;
    private final CostAccountingService costService;
    private final PaymentCollectionService paymentService;
    private final ProfitAnalysisService profitService;
    private final CrmCostAccountingMapper costMapper;
    private final CrmCostSegmentMapper costSegmentMapper;
    private final CrmPaymentPlanMapper paymentPlanMapper;
    private final CrmProfitAnalysisMapper profitMapper;
    private final CrmOrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public FinanceWebController(ReceivablePayableService receivableService,
                                CostAccountingService costService,
                                PaymentCollectionService paymentService,
                                ProfitAnalysisService profitService,
                                CrmCostAccountingMapper costMapper,
                                CrmCostSegmentMapper costSegmentMapper,
                                CrmPaymentPlanMapper paymentPlanMapper,
                                CrmProfitAnalysisMapper profitMapper,
                                CrmOrderMapper orderMapper,
                                ObjectMapper objectMapper) {
        this.receivableService = receivableService;
        this.costService = costService;
        this.paymentService = paymentService;
        this.profitService = profitService;
        this.costMapper = costMapper;
        this.costSegmentMapper = costSegmentMapper;
        this.paymentPlanMapper = paymentPlanMapper;
        this.profitMapper = profitMapper;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/receivables")
    @Operation(summary = "应收账款分页列表")
    public Result<Map<String, Object>> listReceivables(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String customer) {
        return receivableService.listReceivables(customer, pageNum, pageSize);
    }

    @GetMapping("/receivables/{id}")
    @Operation(summary = "应收详情")
    public Result<Map<String, Object>> receivableDetail(@PathVariable Long id) {
        return receivableService.getReceivableDetail(id);
    }

    @PostMapping("/receivables")
    @Operation(summary = "创建应收（Web 路径）")
    public Result<CrmReceivable> createReceivable(@RequestBody CreateReceivableRequest req) {
        Long userId = CurrentUserHelper.currentUserId();
        return receivableService.createReceivable(req, userId == null ? 701L : userId);
    }

    @PostMapping("/receivables/{id}/receipt")
    @Operation(summary = "登记收款（Web 路径）")
    public Result<Map<String, Object>> recordReceivableReceipt(@PathVariable Long id,
                                                                @RequestBody RecordPaymentRequest req) {
        req.setType("RECEIPT");
        req.setRefId(id);
        Long userId = CurrentUserHelper.currentUserId();
        if (req.getPaidBy() == null) {
            req.setPaidBy(userId == null ? 701L : userId);
        }
        return receivableService.recordPayment(req, userId == null ? 701L : userId);
    }

    @GetMapping("/payables")
    @Operation(summary = "应付账款分页列表")
    public Result<Map<String, Object>> listPayables(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String vendor) {
        return receivableService.listPayables(vendor, pageNum, pageSize);
    }

    @GetMapping("/payables/{id}")
    @Operation(summary = "应付详情")
    public Result<Map<String, Object>> payableDetail(@PathVariable Long id) {
        return receivableService.getPayableDetail(id);
    }

    @GetMapping("/aging")
    @Operation(summary = "账龄分析（Web 路径）")
    public Result<Map<String, Object>> listAgings(
            @RequestParam(required = false, defaultValue = "RECEIVABLE") String type,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Result<Map<String, Object>> raw = receivableService.getAging();
        if (!raw.isSuccess() || raw.getData() == null) {
            return raw;
        }
        Map<String, Object> data = raw.getData();
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> buckets = (Map<String, BigDecimal>) (
                "PAYABLE".equalsIgnoreCase(type) ? data.get("payable_by_bucket") : data.get("receivable_by_bucket"));
        BigDecimal total = buckets == null ? BigDecimal.ZERO
                : buckets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (buckets != null) {
            int idx = 1;
            for (Map.Entry<String, BigDecimal> e : buckets.entrySet()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", idx++);
                row.put("bucket", e.getKey());
                row.put("amount", e.getValue());
                row.put("count", e.getValue() != null && e.getValue().signum() > 0 ? 1 : 0);
                row.put("percentage", total.signum() == 0 ? 0
                        : e.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP));
                rows.add(row);
            }
        }
        return Result.ok(pageSlice(rows, pageNum, pageSize));
    }

    @GetMapping("/aging/{id}")
    @Operation(summary = "账龄段明细")
    public Result<Map<String, Object>> agingDetail(@PathVariable Long id) {
        Result<Map<String, Object>> raw = receivableService.getAging();
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", id);
        if (raw.isSuccess() && raw.getData() != null) {
            detail.putAll(raw.getData());
        }
        return Result.ok(detail);
    }

    @GetMapping({"/cost", "/cost-accounting"})
    @Operation(summary = "成本核算列表（Web/OpenAPI 路径别名）")
    public Result<Map<String, Object>> listCosts(
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<CrmCostAccounting> all = costMapper.selectAll();
        List<Map<String, Object>> rows = new ArrayList<>();
        if (all != null) {
            for (CrmCostAccounting c : all) {
                if (period != null && !period.isBlank() && c.getCostDate() != null
                        && !c.getCostDate().toString().startsWith(period)) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("id", c.getId());
                row.put("costNo", c.getCostNo());
                row.put("refType", c.getRefType());
                row.put("refId", c.getRefId());
                row.put("refNo", c.getRefNo());
                row.put("workorderNo", "WORKORDER".equals(c.getRefType()) ? c.getRefNo() : c.getRefNo());
                row.put("materialCode", c.getMaterialCode());
                row.put("materialName", c.getMaterialName());
                row.put("totalAmount", c.getTotalCost());
                row.put("totalCost", c.getTotalCost());
                row.put("costDate", c.getCostDate());
                row.put("status", c.getStatus());
                BigDecimal materialCost = BigDecimal.ZERO;
                BigDecimal laborCost = BigDecimal.ZERO;
                BigDecimal outsourceCost = BigDecimal.ZERO;
                BigDecimal overhead = BigDecimal.ZERO;
                List<CrmCostSegment> segs = costSegmentMapper.selectByCostId(c.getId());
                if (segs != null) {
                    for (CrmCostSegment seg : segs) {
                        if (seg.getAmount() == null) continue;
                        String code = seg.getSegmentCode() != null ? seg.getSegmentCode() : "";
                        switch (code) {
                            case "MATERIAL" -> materialCost = materialCost.add(seg.getAmount());
                            case "PROCESS" -> laborCost = laborCost.add(seg.getAmount());
                            case "OUTSOURCE" -> outsourceCost = outsourceCost.add(seg.getAmount());
                            case "MANAGE", "DEPRECIATION" -> overhead = overhead.add(seg.getAmount());
                            default -> { }
                        }
                    }
                }
                row.put("materialCost", materialCost);
                row.put("laborCost", laborCost);
                row.put("outsourceCost", outsourceCost);
                row.put("overhead", overhead);
                rows.add(row);
            }
        }
        return Result.ok(pageSlice(rows, pageNum, pageSize));
    }

    @GetMapping("/cost/{id}")
    @Operation(summary = "成本核算详情")
    public Result<Map<String, Object>> costDetail(@PathVariable Long id) {
        CrmCostAccounting c = costMapper.selectById(id);
        if (c == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "成本核算单不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("accounting", c);
        List<CrmCostSegment> segs = costSegmentMapper.selectByCostId(c.getId());
        data.put("segments", segs);
        return Result.ok(data);
    }

    @PostMapping("/cost/run")
    @Operation(summary = "触发成本归集（Web 路径）")
    public Result<Map<String, Object>> runCost(@RequestBody(required = false) Map<String, Object> payload) {
        Long userId = CurrentUserHelper.currentUserId();
        long uid = userId == null ? 703L : userId;
        if (payload != null && payload.get("refType") != null) {
            AggregateCostRequest req = objectMapper.convertValue(payload, AggregateCostRequest.class);
            return costService.aggregateCost(req, uid);
        }
        return costService.getCostBySegment();
    }

    @GetMapping({"/payments", "/payment-plan"})
    @Operation(summary = "回款计划列表（Web 路径）")
    public Result<Map<String, Object>> listPayments(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<CrmPaymentPlan> all = paymentPlanMapper.selectAll();
        List<Map<String, Object>> rows = new ArrayList<>();
        if (all != null) {
            for (CrmPaymentPlan p : all) {
                if (!paymentInSalesScope(p)) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("id", p.getId());
                row.put("planNo", p.getPlanNo());
                row.put("orderNo", p.getOrderNo());
                row.put("plannedAmount", p.getPlannedAmount());
                row.put("paidAmount", p.getPaidAmount());
                row.put("plannedDate", p.getPlannedDate());
                row.put("alertLevel", p.getAlertLevel());
                rows.add(row);
            }
        }
        return Result.ok(pageSlice(rows, pageNum, pageSize));
    }

    @GetMapping("/payments/{id}")
    @Operation(summary = "回款计划详情")
    public Result<CrmPaymentPlan> paymentDetail(@PathVariable Long id) {
        CrmPaymentPlan p = paymentPlanMapper.selectById(id);
        if (p == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "回款计划不存在");
        }
        if (!paymentInSalesScope(p)) {
            return Result.fail(40302, "DATA_SCOPE");
        }
        return Result.ok(p);
    }

    @PostMapping("/payments/apply")
    @Operation(summary = "申请/创建回款计划（Web 路径）")
    public Result<CrmPaymentPlan> applyPayment(@RequestBody CreatePlanRequest req) {
        Long userId = CurrentUserHelper.currentUserId();
        return paymentService.createPlan(req, userId == null ? 701L : userId);
    }

    @PostMapping("/payments/{id}/approve")
    @Operation(summary = "审批并标记回款（Web 路径）")
    public Result<CrmPaymentPlan> approvePayment(@PathVariable Long id,
                                                  @RequestBody(required = false) MarkPaidRequest req) {
        CrmPaymentPlan plan = paymentPlanMapper.selectById(id);
        if (plan == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "回款计划不存在");
        }
        if (req == null) {
            req = new MarkPaidRequest();
        }
        if (req.getPaidAmount() == null) {
            BigDecimal planned = plan.getPlannedAmount() == null ? BigDecimal.ZERO : plan.getPlannedAmount();
            BigDecimal paid = plan.getPaidAmount() == null ? BigDecimal.ZERO : plan.getPaidAmount();
            req.setPaidAmount(planned.subtract(paid));
        }
        Long userId = CurrentUserHelper.currentUserId();
        if (req.getPaidBy() == null) {
            req.setPaidBy(userId == null ? 701L : userId);
        }
        return paymentService.markPaid(id, req, userId == null ? 701L : userId);
    }

    @GetMapping("/profit")
    @Operation(summary = "利润分析列表（Web 路径）")
    public Result<Map<String, Object>> listProfits(
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<CrmProfitAnalysis> all = period == null || period.isBlank()
                ? profitMapper.selectAllScoped(
                        SalesDataScopeHelper.resolveListOwnerUserId(),
                        SalesDataScopeHelper.resolveListDeptId())
                : profitMapper.selectByMonthScoped(
                        period,
                        SalesDataScopeHelper.resolveListOwnerUserId(),
                        SalesDataScopeHelper.resolveListDeptId());
        List<Map<String, Object>> rows = new ArrayList<>();
        if (all != null) {
            for (CrmProfitAnalysis p : all) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", p.getId());
                row.put("profitNo", p.getProfitNo());
                row.put("orderNo", p.getOrderNo());
                row.put("customerName", p.getCustomerName());
                row.put("revenue", p.getRevenue());
                row.put("totalCost", p.getTotalCost());
                row.put("profit", p.getProfit());
                row.put("profitRate", p.getProfitRate());
                row.put("analysisMonth", p.getAnalysisMonth());
                rows.add(row);
            }
        }
        return Result.ok(pageSlice(rows, pageNum, pageSize));
    }

    @GetMapping("/profit/export")
    @Operation(summary = "利润报告导出（Web 路径）")
    public ResponseEntity<byte[]> exportProfit(@RequestParam(required = false) String period) {
        Result<Map<String, Object>> raw = profitService.exportProfitReport(period, 1L);
        String body = raw.isSuccess() ? String.valueOf(raw.getData()) : raw.getMessage();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=profit-report.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(body == null ? new byte[0] : body.getBytes());
    }

    /** 业务员/销售经理访问回款计划时，按关联订单 owner/dept 过滤；财务 ALL 不过滤 */
    private boolean paymentInSalesScope(CrmPaymentPlan plan) {
        if (SalesDataScopeHelper.effectiveScope() == SalesDataScopeHelper.Scope.ALL) {
            return true;
        }
        if (plan.getOrderId() == null) {
            return false;
        }
        CrmOrder order = orderMapper.selectById(plan.getOrderId());
        if (order == null) {
            return false;
        }
        return SalesDataScopeHelper.assertOwnerDept(order.getOwnerUserId(), order.getDeptId()).isSuccess();
    }

    private static Map<String, Object> pageSlice(List<Map<String, Object>> all, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 20;
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, all.size());
        List<Map<String, Object>> slice = from < all.size() ? all.subList(from, to) : List.of();
        Map<String, Object> page = new HashMap<>();
        page.put("items", slice);
        page.put("records", slice);
        page.put("list", slice);
        page.put("total", all.size());
        page.put("pageNum", pageNum);
        page.put("pageSize", pageSize);
        return page;
    }
}
