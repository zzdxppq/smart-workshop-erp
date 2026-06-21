package com.btsheng.erp.business.crm.engineer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.business.crm.engineer.entity.CrmEngineeringWorkbench;
import com.btsheng.erp.business.crm.engineer.mapper.CrmEngineeringWorkbenchMapper;
import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * V2.1 · 工程师待办任务聚合（报价工艺定义 + 订单工程转化）
 */
@Service
public class EngineerTaskService {

    private static final String PHASE_PENDING = "PENDING";
    private static final String PHASE_IN_PROGRESS = "IN_PROGRESS";
    private static final String PHASE_COMPLETED = "COMPLETED";

    private final CrmQuoteMapper quoteMapper;
    private final CrmQuoteItemMapper quoteItemMapper;
    private final CrmOrderMapper orderMapper;
    private final CrmEngineeringWorkbenchMapper workbenchMapper;

    @Autowired
    public EngineerTaskService(CrmQuoteMapper quoteMapper,
                               CrmQuoteItemMapper quoteItemMapper,
                               CrmOrderMapper orderMapper,
                               CrmEngineeringWorkbenchMapper workbenchMapper) {
        this.quoteMapper = quoteMapper;
        this.quoteItemMapper = quoteItemMapper;
        this.orderMapper = orderMapper;
        this.workbenchMapper = workbenchMapper;
    }

    public static String computeQuotePhase(List<CrmQuoteItem> items, String quoteStatus) {
        if ("CONVERTED".equals(quoteStatus)) {
            return PHASE_COMPLETED;
        }
        if (items == null || items.isEmpty()) {
            return PHASE_PENDING;
        }
        long filled = items.stream()
                .filter(i -> i.getProcessSummary() != null && !i.getProcessSummary().isBlank())
                .count();
        if (filled == 0) {
            return PHASE_PENDING;
        }
        if (filled == items.size()) {
            return PHASE_COMPLETED;
        }
        return PHASE_IN_PROGRESS;
    }

    public static String computeOrderPhase(List<CrmEngineeringWorkbench> workbenches, String orderStatus) {
        if (Set.of("PENDING_PRODUCTION", "IN_PRODUCTION", "PARTIAL_SHIPPED", "SHIPPED",
                "SETTLED", "CLOSED", "PRODUCING", "CONFIRMED").contains(orderStatus)) {
            return PHASE_COMPLETED;
        }
        if (workbenches == null || workbenches.isEmpty()) {
            return PHASE_PENDING;
        }
        if (workbenches.stream().allMatch(w -> PHASE_COMPLETED.equals(w.getStatus()))) {
            return PHASE_COMPLETED;
        }
        boolean inProgress = workbenches.stream().anyMatch(w ->
                PHASE_IN_PROGRESS.equals(w.getStatus())
                        || PHASE_IN_PROGRESS.equals(w.getProcessStatus())
                        || PHASE_IN_PROGRESS.equals(w.getBomStatus())
                        || (w.getProcessDetail() != null && !w.getProcessDetail().isBlank())
                        || (w.getBomDetail() != null && !w.getBomDetail().isBlank()));
        return inProgress ? PHASE_IN_PROGRESS : PHASE_PENDING;
    }

    public Result<Map<String, Object>> listQuoteEngineerQueue(int pageNum, int pageSize, String phase,
                                                                Long customerId, String dateFrom, String dateTo) {
        QueryWrapper<CrmQuote> qw = new QueryWrapper<>();
        qw.in("status", "PENDING_ENG", "SUBMITTED", "APPROVED", "CONVERTED");
        qw.eq("is_deleted", 0);
        if (customerId != null) {
            qw.eq("customer_id", customerId);
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            qw.ge("created_at", dateFrom + " 00:00:00");
        }
        if (dateTo != null && !dateTo.isBlank()) {
            qw.le("created_at", dateTo + " 23:59:59");
        }
        qw.orderByDesc("created_at");
        List<CrmQuote> all = quoteMapper.selectList(qw);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (CrmQuote quote : all) {
            List<CrmQuoteItem> items = quoteItemMapper.selectByQuoteId(quote.getId());
            String engineerPhase = computeQuotePhase(items, quote.getStatus());
            if (phase != null && !phase.isBlank() && !phase.equals(engineerPhase)) {
                continue;
            }
            Map<String, Object> row = quoteToMap(quote);
            row.put("engineerPhase", engineerPhase);
            row.put("itemCount", items.size());
            rows.add(row);
        }

        return paginate(rows, pageNum, pageSize);
    }

    public Result<Map<String, Object>> listOrderEngineerQueue(int pageNum, int pageSize, String phase) {
        QueryWrapper<CrmOrder> qw = new QueryWrapper<>();
        qw.in("status", "APPROVED", "PROCESSING", "PENDING_PRODUCTION",
                "IN_PRODUCTION", "PARTIAL_SHIPPED", "SHIPPED", "SETTLED", "CLOSED",
                "CONFIRMED", "PRODUCING");
        qw.eq("is_deleted", 0);
        qw.orderByDesc("created_at");
        List<CrmOrder> all = orderMapper.selectList(qw);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (CrmOrder order : all) {
            List<CrmEngineeringWorkbench> wbs = workbenchMapper.selectByOrderId(order.getId());
            String engineerPhase = computeOrderPhase(wbs, order.getStatus());
            if (phase != null && !phase.isBlank() && !phase.equals(engineerPhase)) {
                continue;
            }
            Map<String, Object> row = orderToMap(order);
            row.put("engineerPhase", engineerPhase);
            row.put("workbenchCount", wbs.size());
            if (!wbs.isEmpty()) {
                row.put("workbenchId", wbs.get(0).getId());
            }
            rows.add(row);
        }

        return paginate(rows, pageNum, pageSize);
    }

    public Result<List<Map<String, Object>>> listUnifiedTasks(String phase) {
        List<Map<String, Object>> tasks = new ArrayList<>();

        QueryWrapper<CrmQuote> quoteQw = new QueryWrapper<>();
        quoteQw.in("status", "PENDING_ENG");
        quoteQw.eq("is_deleted", 0);
        quoteQw.orderByAsc("created_at");
        for (CrmQuote quote : quoteMapper.selectList(quoteQw)) {
            List<CrmQuoteItem> items = quoteItemMapper.selectByQuoteId(quote.getId());
            String engineerPhase = computeQuotePhase(items, quote.getStatus());
            if (PHASE_COMPLETED.equals(engineerPhase)) {
                continue;
            }
            if (phase != null && !phase.isBlank() && !phase.equals(engineerPhase)) {
                continue;
            }
            tasks.add(buildTask("QUOTE_PROCESS", quote.getId(), quote.getQuoteNo(),
                    quote.getCustomerName(), engineerPhase, quote.getCreatedAt(), 2));
        }

        java.util.LinkedHashSet<Long> seenOrderIds = new java.util.LinkedHashSet<>();
        for (CrmEngineeringWorkbench wb : workbenchMapper.selectAllPending()) {
            if (!seenOrderIds.add(wb.getOrderId())) {
                continue;
            }
            CrmOrder order = orderMapper.selectById(wb.getOrderId());
            if (order == null) {
                continue;
            }
            List<CrmEngineeringWorkbench> wbs = workbenchMapper.selectByOrderId(order.getId());
            String engineerPhase = computeOrderPhase(wbs, order.getStatus());
            if (PHASE_COMPLETED.equals(engineerPhase)) {
                continue;
            }
            if (phase != null && !phase.isBlank() && !phase.equals(engineerPhase)) {
                continue;
            }
            Map<String, Object> task = buildTask("ORDER_CONVERSION", order.getId(), order.getOrderNo(),
                    order.getCustomerName(), engineerPhase, wb.getCreatedAt(), 3);
            task.put("workbenchId", wb.getId());
            tasks.add(task);
        }

        tasks.sort((a, b) -> {
            int p = String.valueOf(a.get("phase")).compareTo(String.valueOf(b.get("phase")));
            if (p != 0) {
                return p;
            }
            return String.valueOf(a.get("refNo")).compareTo(String.valueOf(b.get("refNo")));
        });

        return Result.ok(tasks);
    }

    private Map<String, Object> buildTask(String source, Long refId, String refNo, String title,
                                          String engineerPhase, LocalDateTime createdAt, int dueDays) {
        Map<String, Object> task = new HashMap<>();
        task.put("source", source);
        task.put("refId", refId);
        task.put("refNo", refNo);
        task.put("title", title != null ? title : refNo);
        task.put("phase", engineerPhase);
        LocalDate due = (createdAt != null ? createdAt.toLocalDate() : LocalDate.now()).plusDays(dueDays);
        task.put("dueDate", due.toString());
        task.put("updatedAt", createdAt != null ? createdAt.toString() : null);
        return task;
    }

    private Map<String, Object> quoteToMap(CrmQuote quote) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", quote.getId());
        row.put("quoteNo", quote.getQuoteNo());
        row.put("customerId", quote.getCustomerId());
        row.put("customerName", quote.getCustomerName());
        row.put("totalAmount", quote.getTotalAmount());
        row.put("status", quote.getStatus());
        row.put("ownerUserId", quote.getOwnerUserId());
        row.put("createdAt", quote.getCreatedAt());
        return row;
    }

    private Map<String, Object> orderToMap(CrmOrder order) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", order.getId());
        row.put("orderNo", order.getOrderNo());
        row.put("customerId", order.getCustomerId());
        row.put("customerName", order.getCustomerName());
        row.put("amount", order.getTotalAmount());
        row.put("status", order.getStatus());
        row.put("ownerUserId", order.getOwnerUserId());
        row.put("createdAt", order.getCreatedAt());
        return row;
    }

    private Result<Map<String, Object>> paginate(List<Map<String, Object>> rows, int pageNum, int pageSize) {
        int total = rows.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        List<Map<String, Object>> page = from >= total ? List.of() : rows.subList(from, to);

        Map<String, Object> result = new HashMap<>();
        result.put("records", page);
        result.put("items", page);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return Result.ok(result);
    }
}
