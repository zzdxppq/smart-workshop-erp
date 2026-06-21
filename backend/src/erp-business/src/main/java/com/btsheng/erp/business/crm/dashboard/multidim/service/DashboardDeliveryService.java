package com.btsheng.erp.business.crm.dashboard.multidim.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.integration.client.WorkorderClient;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** E11-S2 AC-11.2.3 · 交付期检索 */
@Service
public class DashboardDeliveryService {

    private final CrmOrderMapper orderMapper;
    private final WorkorderClient workorderClient;

    @Autowired
    public DashboardDeliveryService(CrmOrderMapper orderMapper, WorkorderClient workorderClient) {
        this.orderMapper = orderMapper;
        this.workorderClient = workorderClient;
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> search(String customerKeyword,
                                               List<String> statuses,
                                               String deliveryFrom,
                                               String deliveryTo) {
        QueryWrapper<CrmOrder> qw = new QueryWrapper<>();
        qw.eq("is_deleted", 0);
        if (customerKeyword != null && !customerKeyword.isBlank()) {
            qw.like("customer_name", customerKeyword.trim());
        }
        if (deliveryFrom != null && !deliveryFrom.isBlank()) {
            qw.ge("delivery_date", LocalDate.parse(deliveryFrom));
        }
        if (deliveryTo != null && !deliveryTo.isBlank()) {
            qw.le("delivery_date", LocalDate.parse(deliveryTo));
        }
        qw.orderByAsc("delivery_date").last("LIMIT 200");
        List<CrmOrder> orders = orderMapper.selectList(qw);

        Set<String> statusFilter = statuses == null ? Set.of() :
                Set.copyOf(statuses.stream().map(s -> s.toUpperCase(Locale.ROOT)).toList());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (CrmOrder o : orders) {
            String deliveryStatus = mapDeliveryStatus(o);
            if (!statusFilter.isEmpty() && !statusFilter.contains(deliveryStatus)) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("orderId", o.getId());
            row.put("orderNo", o.getOrderNo());
            row.put("customerName", o.getCustomerName());
            row.put("status", deliveryStatus);
            row.put("orderStatus", o.getStatus());
            row.put("progress", estimateProgress(o));
            row.put("plannedDelivery", o.getDeliveryDate());
            row.put("actualDelivery", "SHIPPED".equals(o.getStatus()) || "SETTLED".equals(o.getStatus())
                    ? o.getDeliveryDate() : null);
            enrichWorkorder(row, o.getId());
            row.put("salesmanId", o.getOwnerUserId());
            rows.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("total", rows.size());
        return Result.ok(data);
    }

    private void enrichWorkorder(Map<String, Object> row, Long orderId) {
        Result<Map<String, Object>> woRes = workorderClient.getBySalesOrderId(orderId);
        if (woRes != null && woRes.isSuccess() && woRes.getData() != null) {
            Map<String, Object> wo = woRes.getData();
            row.put("workorderNo", wo.get("workorderNo"));
            row.put("materialCode", wo.get("materialCode"));
            row.put("productName", wo.get("productName"));
            Object progress = wo.get("progress");
            if (progress != null) {
                row.put("progress", progress);
            }
            row.put("currentStep", wo.getOrDefault("currentStep", currentStepLabel(String.valueOf(row.get("orderStatus")))));
        } else {
            row.put("currentStep", currentStepLabel(String.valueOf(row.get("orderStatus"))));
        }
    }

    public Result<Map<String, Object>> buildFeedbackTemplate(Long orderId, String customerName,
                                                             String orderNo, String currentStep,
                                                             LocalDate plannedDelivery) {
        if (orderId != null) {
            CrmOrder o = orderMapper.selectById(orderId);
            if (o != null) {
                customerName = o.getCustomerName();
                orderNo = o.getOrderNo();
                currentStep = currentStepLabel(o.getStatus());
                plannedDelivery = o.getDeliveryDate();
            }
        }
        String text = String.format("尊敬的客户 %s，您的订单 %s 当前工序：%s，预计交期：%s。如有疑问请联系我司业务员。",
                customerName != null ? customerName : "客户",
                orderNo != null ? orderNo : "—",
                currentStep != null ? currentStep : "生产中",
                plannedDelivery != null ? plannedDelivery : "待确认");
        Map<String, Object> out = new HashMap<>();
        out.put("template", text);
        return Result.ok(out);
    }

    private String mapDeliveryStatus(CrmOrder o) {
        String st = o.getStatus() != null ? o.getStatus() : "";
        if ("SHIPPED".equals(st) || "SETTLED".equals(st) || "CLOSED".equals(st)) {
            return "COMPLETED";
        }
        if (o.getDeliveryDate() != null && o.getDeliveryDate().isBefore(LocalDate.now())
                && !"SHIPPED".equals(st)) {
            return "OVERDUE";
        }
        return "PENDING_DELIVERY";
    }

    private int estimateProgress(CrmOrder o) {
        return switch (o.getStatus() != null ? o.getStatus() : "") {
            case "DRAFT", "CONFIRMED" -> 10;
            case "PRODUCING" -> 55;
            case "PARTIAL_SHIPPED" -> 80;
            case "SHIPPED", "SETTLED", "CLOSED" -> 100;
            default -> 30;
        };
    }

    private String currentStepLabel(String status) {
        return switch (status != null ? status : "") {
            case "DRAFT" -> "待确认";
            case "CONFIRMED" -> "已确认待投产";
            case "PRODUCING" -> "生产中";
            case "PARTIAL_SHIPPED" -> "部分发货";
            case "SHIPPED" -> "已发货";
            case "SETTLED", "CLOSED" -> "已完结";
            default -> "跟进中";
        };
    }
}
