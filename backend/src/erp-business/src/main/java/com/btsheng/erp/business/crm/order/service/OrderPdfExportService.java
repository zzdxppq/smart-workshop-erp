package com.btsheng.erp.business.crm.order.service;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.entity.CrmOrderHistory;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import com.btsheng.erp.business.crm.order.entity.CrmOrderPayment;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderHistoryMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderItemMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderPaymentMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V1.3.7 · Story 1.6 · AC-2.3.4 · 订单 PDF/Excel 导出 Service
 *
 * <p>复用 Story 1.5 PdfExportService 1h 缓存模式（ConcurrentHashMap + cacheTimestamp�? * <br>部署阶段选型 OpenPDF 1.3.34 / POI 5.2.5，本 Story 简化为文本兜底
 */
@Service
public class OrderPdfExportService {

    private final CrmOrderMapper orderMapper;
    private final CrmOrderItemMapper itemMapper;
    private final CrmOrderHistoryMapper historyMapper;
    private final CrmOrderPaymentMapper paymentMapper;
    private final OrderService orderService;

    // 1h PDF 缓存（V1.3.7 T4.6�?
            private final ConcurrentHashMap<Long, byte[]> pdfCache = new ConcurrentHashMap<>();
    private final long cacheTtlMillis = 3600_000L;
    private final ConcurrentHashMap<Long, Long> cacheTimestamp = new ConcurrentHashMap<>();

    @Autowired
    public OrderPdfExportService(CrmOrderMapper orderMapper, CrmOrderItemMapper itemMapper,
                                 CrmOrderHistoryMapper historyMapper, CrmOrderPaymentMapper paymentMapper,
                                 OrderService orderService) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.historyMapper = historyMapper;
        this.paymentMapper = paymentMapper;
        this.orderService = orderService;
    }

    @AuditLog(module = "order", action = "order.pdf_download")
    public Result<byte[]> exportPdf(Long orderId, Long operatorUserId) {
        CrmOrder order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        List<CrmOrderItem> items = itemMapper.selectByOrderId(orderId);

        // 1h 缓存
            byte[] cached = getCachedPdf(orderId);
        if (cached != null) {
            recordExport(orderId, operatorUserId, "PDF_CACHE_HIT " + cached.length + " bytes");
            return Result.ok(cached);
        }

        // 简化：构�?PDF 文本（部署阶段换 OpenPDF 1.3.34�?
            StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n%昆山佰泰胜精密机械有限公司\n");
        sb.append("订单�? ").append(order.getOrderNo()).append("\n");
        sb.append("客户: ").append(order.getCustomerName()).append("\n");
        sb.append("金额: ¥").append(order.getTotalAmount()).append(" ").append(order.getCurrency()).append("\n");
        sb.append("交期: ").append(order.getDeliveryDate()).append("\n");
        sb.append("状�? ").append(order.getStatus()).append("\n");
        sb.append("生产工单: ").append(order.getProductionOrderNo() == null ? "-" : order.getProductionOrderNo()).append("\n");
        sb.append("委外工单: ").append(order.getOutsourceOrderNo() == null ? "-" : order.getOutsourceOrderNo()).append("\n");
        sb.append("审批签字: _______________\n");
        sb.append("审批时间: ").append(LocalDateTime.now()).append("\n");
        sb.append("明细:\n");
        for (CrmOrderItem item : items) {
            sb.append("  图号=").append(item.getDrawingNo())
              .append(" 数量=").append(item.getQuantity())
              .append(" 单价=").append(item.getUnitPrice())
              .append(" 金额=").append(item.getAmount())
              .append(" 已发=").append(item.getShippedQty()).append("\n");
        }
        sb.append("%%EOF\n");
        byte[] bytes = sb.toString().getBytes();
        pdfCache.put(orderId, bytes);
        cacheTimestamp.put(orderId, System.currentTimeMillis());

        recordExport(orderId, operatorUserId, "PDF_FRESH " + bytes.length + " bytes");
        return Result.ok(bytes);
    }

    @AuditLog(module = "order", action = "order.excel_download")
    public Result<byte[]> exportExcel(Long orderId, Long operatorUserId) {
        CrmOrder order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        List<CrmOrderItem> items = itemMapper.selectByOrderId(orderId);
        List<CrmOrderHistory> histories = historyMapper.selectByOrderId(orderId);
        List<CrmOrderPayment> payments = paymentMapper.selectByOrderId(orderId);

        // 简化：构�?Excel �?Sheet 文本（部署阶段换 Apache POI 5.2.5�?
            StringBuilder sb = new StringBuilder();
        sb.append("Sheet1: 订单基本信息\n");
        sb.append("订单�?客户,金额,币种,交期,状�?生产工单,委外工单\n");
        sb.append(order.getOrderNo()).append(",")
          .append(order.getCustomerName()).append(",")
          .append(order.getTotalAmount()).append(",")
          .append(order.getCurrency()).append(",")
          .append(order.getDeliveryDate()).append(",")
          .append(order.getStatus()).append(",")
          .append(order.getProductionOrderNo() == null ? "" : order.getProductionOrderNo()).append(",")
          .append(order.getOutsourceOrderNo() == null ? "" : order.getOutsourceOrderNo()).append("\n");

        sb.append("\nSheet2: 订单明细\n");
        sb.append("图号,材质,数量,单价,金额,数量调整,已发,是否FA,是否新件\n");
        for (CrmOrderItem item : items) {
            sb.append(item.getDrawingNo()).append(",")
              .append(item.getMaterial()).append(",")
              .append(item.getQuantity()).append(",")
              .append(item.getUnitPrice()).append(",")
              .append(item.getAmount()).append(",")
              .append(item.getQuantityAdjustment()).append(",")
              .append(item.getShippedQty()).append(",")
              .append(item.getIsFa()).append(",")
              .append(item.getIsNew()).append("\n");
        }

        sb.append("\nSheet3: 审批记录\n");
        sb.append("操作,操作�?操作时间\n");
        for (CrmOrderHistory h : histories) {
            sb.append(h.getOperation()).append(",")
              .append(h.getChangedBy()).append(",")
              .append(h.getChangedAt()).append("\n");
        }

        sb.append("\nSheet4: 发货记录\n");
        sb.append("回款单号,金额,日期,状态\n");
        for (CrmOrderPayment p : payments) {
            sb.append(p.getPaymentNo()).append(",")
              .append(p.getAmount()).append(",")
              .append(p.getPaymentDate()).append(",")
              .append(p.getStatus()).append("\n");
        }

        recordExport(orderId, operatorUserId, "EXCEL " + sb.length() + " bytes");
        return Result.ok(sb.toString().getBytes());
    }

    private byte[] getCachedPdf(Long orderId) {
        Long ts = cacheTimestamp.get(orderId);
        if (ts == null) return null;
        if (System.currentTimeMillis() - ts > cacheTtlMillis) {
            pdfCache.remove(orderId);
            cacheTimestamp.remove(orderId);
            return null;
        }
        return pdfCache.get(orderId);
    }

    private void recordExport(Long orderId, Long userId, String info) {
        CrmOrderHistory h = new CrmOrderHistory();
        h.setOrderId(orderId);
        h.setOperation("PDF_DOWNLOAD");
        h.setChangedBy(userId);
        h.setChangedAt(LocalDateTime.now());
        h.setAfterJson(info);
        historyMapper.insert(h);
    }
}
