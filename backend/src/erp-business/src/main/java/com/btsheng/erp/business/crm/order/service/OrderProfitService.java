package com.btsheng.erp.business.crm.order.service;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.entity.CrmOrderHistory;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderHistoryMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderItemMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.6 · 订单利润分析 Service
 *
 * <p>利润 = 订单金额 - 生产成本 - 委外成本 - 材料成本
 * <br>实际生产/委外/材料成本�?Epic 5/6/8 服务提供（Feign 客户端），本 Story 简化为占位
 * <br>利润 < 0 触发告警
 */
@Service
public class OrderProfitService {

    private final CrmOrderMapper orderMapper;
    private final CrmOrderItemMapper itemMapper;
    private final CrmOrderHistoryMapper historyMapper;

    // 估算系数（部署阶段由 Epic 5/6/8 真实成本替代�?
            private static final BigDecimal PRODUCTION_COST_RATIO = new BigDecimal("0.55");
    private static final BigDecimal OUTSOURCE_COST_RATIO = new BigDecimal("0.65");
    private static final BigDecimal MATERIAL_COST_RATIO = new BigDecimal("0.20");

    @Autowired
    public OrderProfitService(CrmOrderMapper orderMapper, CrmOrderItemMapper itemMapper,
                              CrmOrderHistoryMapper historyMapper) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.historyMapper = historyMapper;
    }

    @AuditLog(module = "order", action = "order.profit_analysis")
    public Result<Map<String, Object>> analyzeProfit(Long orderId, Long operatorUserId) {
        CrmOrder order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        List<CrmOrderItem> items = itemMapper.selectByOrderId(orderId);

        BigDecimal orderAmount = order.getTotalAmount();
        // 估算成本（部署阶段由 Epic 5/6/8 真实成本替代�?
            boolean hasOutsource = order.getOutsourceOrderNo() != null;
        boolean hasProduction = order.getProductionOrderNo() != null;

        BigDecimal productionCost = BigDecimal.ZERO;
        BigDecimal outsourceCost = BigDecimal.ZERO;
        BigDecimal materialCost = orderAmount.multiply(MATERIAL_COST_RATIO).setScale(2, RoundingMode.HALF_UP);

        if (hasProduction) {
            productionCost = orderAmount.multiply(PRODUCTION_COST_RATIO).setScale(2, RoundingMode.HALF_UP);
        }
        if (hasOutsource) {
            outsourceCost = orderAmount.multiply(OUTSOURCE_COST_RATIO).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalCost = productionCost.add(outsourceCost).add(materialCost);
        BigDecimal profit = orderAmount.subtract(totalCost);
        BigDecimal profitRate = orderAmount.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : profit.divide(orderAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("orderNo", order.getOrderNo());
        result.put("orderAmount", orderAmount);
        result.put("productionCost", productionCost);
        result.put("outsourceCost", outsourceCost);
        result.put("materialCost", materialCost);
        result.put("totalCost", totalCost);
        result.put("profit", profit);
        result.put("profitRate", profitRate + "%");
        result.put("isLoss", profit.compareTo(BigDecimal.ZERO) < 0);
        result.put("alert", profit.compareTo(BigDecimal.ZERO) < 0 ? "NEGATIVE_PROFIT_WARNING" : null);
        result.put("itemCount", items.size());

        return Result.ok(result);
    }

    /**
     * 利润分析 PDF（复�?PdfExportService 模式�?     */
    @AuditLog(module = "order", action = "order.profit_pdf")
    public Result<byte[]> exportProfitPdf(Long orderId, Long operatorUserId) {
        Result<Map<String, Object>> analysis = analyzeProfit(orderId, operatorUserId);
        if (analysis.getCode() != 0) return Result.fail(analysis.getCode(), analysis.getMessage());

        Map<String, Object> data = analysis.getData();
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n%昆山佰泰胜精密机械有限公�?· 订单利润分析\n");
        sb.append("订单�? ").append(data.get("orderNo")).append("\n");
        sb.append("订单金额: ¥").append(data.get("orderAmount")).append("\n");
        sb.append("生产成本: ¥").append(data.get("productionCost")).append("\n");
        sb.append("委外成本: ¥").append(data.get("outsourceCost")).append("\n");
        sb.append("材料成本: ¥").append(data.get("materialCost")).append("\n");
        sb.append("总成�?   ¥").append(data.get("totalCost")).append("\n");
        sb.append("利润:     ¥").append(data.get("profit")).append("\n");
        sb.append("利润�?   ").append(data.get("profitRate")).append("\n");
        if (Boolean.TRUE.equals(data.get("isLoss"))) {
            sb.append("告警: 负利润！需关注成本控制\n");
        }
        sb.append("%%EOF\n");

        // 留痕
            CrmOrderHistory h = new CrmOrderHistory();
        h.setOrderId(orderId);
        h.setOperation("PROFIT_ANALYSIS");
        h.setChangedBy(operatorUserId);
        h.setChangedAt(LocalDateTime.now());
        h.setAfterJson("profit=" + data.get("profit"));
        historyMapper.insert(h);

        return Result.ok(sb.toString().getBytes());
    }
}
