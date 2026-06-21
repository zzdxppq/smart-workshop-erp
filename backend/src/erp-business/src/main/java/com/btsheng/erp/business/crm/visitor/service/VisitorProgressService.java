package com.btsheng.erp.business.crm.visitor.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.integration.client.WorkorderClient;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** V1.4.0 · E11-S7 · 脱敏进度（屏蔽金额/委外） */
@Service
public class VisitorProgressService {

    private final CrmOrderMapper orderMapper;
    private final WorkorderClient workorderClient;

    @Autowired
    public VisitorProgressService(CrmOrderMapper orderMapper, WorkorderClient workorderClient) {
        this.orderMapper = orderMapper;
        this.workorderClient = workorderClient;
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Result.fail(40001, "请输入料号、工单号或客户名");
        }
        String kw = keyword.trim();
        Map<String, Map<String, Object>> dedupe = new LinkedHashMap<>();

        appendFromWorkorders(kw, dedupe);
        appendFromOrders(kw, dedupe);

        List<Map<String, Object>> items = new ArrayList<>(dedupe.values());
        Map<String, Object> data = new HashMap<>();
        data.put("keyword", kw);
        data.put("list", items);
        data.put("total", items.size());
        return Result.ok(data);
    }

    /** V1.4.0 · 默认视图：全厂活跃工单（跨 5 个状态 UNION 取最多 limit 条） */
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> activeList(Integer limit) {
        int n = (limit == null || limit <= 0) ? 23 : Math.min(limit, 50);
        Result<Map<String, Object>> res = workorderClient.visitorActive(n);
        if (res == null || !res.isSuccess() || res.getData() == null) {
            // fallback：用本地订单表按状态估进度
            List<Map<String, Object>> items = new ArrayList<>();
            String[] activeStatuses = {"PRODUCING", "PARTIAL_SHIPPED"};
            for (String st : activeStatuses) {
                QueryWrapper<CrmOrder> qw = new QueryWrapper<>();
                qw.eq("is_deleted", 0).eq("status", st).last("LIMIT " + (n / activeStatuses.length + 2));
                for (CrmOrder o : orderMapper.selectList(qw)) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("orderNo", o.getOrderNo());
                    row.put("customerName", o.getCustomerName());
                    row.put("plannedDelivery", o.getDeliveryDate());
                    row.put("progress", estimateOrderProgress(o.getStatus()));
                    row.put("currentStep", stepLabel(o.getStatus()));
                    row.put("status", o.getStatus());
                    row.put("steps", List.of());
                    items.add(row);
                }
                if (items.size() >= n) break;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("list", items);
            data.put("total", items.size());
            data.put("limit", n);
            return Result.ok(data);
        }
        return res;
    }

    /** V1.4.0 · 工单详情 + 工序时间线（脱敏） */
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> detail(String workorderNo) {
        if (workorderNo == null || workorderNo.isBlank()) {
            return Result.fail(40001, "WORKORDER_NO_REQUIRED");
        }
        return workorderClient.visitorDetail(workorderNo.trim());
    }

    @SuppressWarnings("unchecked")
    private void appendFromWorkorders(String kw, Map<String, Map<String, Object>> dedupe) {
        Result<Map<String, Object>> res = workorderClient.visitorSearch(kw);
        if (res == null || !res.isSuccess() || res.getData() == null) return;
        Object listObj = res.getData().get("list");
        if (!(listObj instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) continue;
            Map<String, Object> row = new HashMap<>((Map<String, Object>) m);
            String woNo = str(row.get("workorderNo"));
            if (woNo != null) {
                enrichCustomerFromOrder(row);
                dedupe.put("wo-" + woNo, row);
            }
        }
    }

    private void appendFromOrders(String kw, Map<String, Map<String, Object>> dedupe) {
        QueryWrapper<CrmOrder> qw = new QueryWrapper<>();
        qw.eq("is_deleted", 0);
        qw.and(w -> w.like("customer_name", kw).or().like("order_no", kw));
        qw.last("LIMIT 20");
        for (CrmOrder o : orderMapper.selectList(qw)) {
            String key = "order-" + o.getOrderNo();
            if (dedupe.values().stream().anyMatch(r -> o.getOrderNo().equals(r.get("orderNo")))) {
                continue;
            }
            Result<Map<String, Object>> woRes = workorderClient.getBySalesOrderId(o.getId());
            if (woRes != null && woRes.isSuccess() && woRes.getData() != null) {
                Map<String, Object> row = new HashMap<>(woRes.getData());
                row.put("orderNo", o.getOrderNo());
                row.put("customerName", o.getCustomerName());
                row.put("plannedDelivery", o.getDeliveryDate());
                String woNo = str(row.get("workorderNo"));
                if (woNo != null) dedupe.put("wo-" + woNo, row);
            } else {
                Map<String, Object> row = new HashMap<>();
                row.put("orderNo", o.getOrderNo());
                row.put("customerName", o.getCustomerName());
                row.put("plannedDelivery", o.getDeliveryDate());
                row.put("progress", estimateOrderProgress(o.getStatus()));
                row.put("currentStep", stepLabel(o.getStatus()));
                row.put("status", o.getStatus());
                row.put("steps", List.of());
                dedupe.putIfAbsent(key, row);
            }
        }
    }

    private void enrichCustomerFromOrder(Map<String, Object> row) {
        String salesOrderNo = str(row.get("salesOrderNo"));
        if (salesOrderNo == null) return;
        QueryWrapper<CrmOrder> qw = new QueryWrapper<>();
        qw.eq("order_no", salesOrderNo).eq("is_deleted", 0).last("LIMIT 1");
        CrmOrder o = orderMapper.selectOne(qw);
        if (o != null) {
            row.put("orderNo", o.getOrderNo());
            row.put("customerName", o.getCustomerName());
            if (row.get("plannedDelivery") == null) {
                row.put("plannedDelivery", o.getDeliveryDate());
            }
        }
    }

    private int estimateOrderProgress(String status) {
        return switch (status != null ? status : "") {
            case "PRODUCING" -> 55;
            case "PARTIAL_SHIPPED" -> 80;
            case "SHIPPED", "SETTLED", "CLOSED" -> 100;
            default -> 20;
        };
    }

    private String stepLabel(String status) {
        return switch (status != null ? status : "") {
            case "PRODUCING" -> "生产中";
            case "SHIPPED" -> "已发货";
            case "CONFIRMED" -> "已确认待投产";
            default -> "跟进中";
        };
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
