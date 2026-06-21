package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteHistory;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteHistoryMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 报价导出 Service（V1.3.7 · Story 1.5 · AC-2.2.3�? *
 * PDF/Excel 导出（V1.3.7 部署阶段选型 OpenPDF 1.3.34 / POI 5.2.5；本 Story 简化为文本格式�? * 1h 缓存�?`app.cache-ttl` 系统参数控制（Story 1.3�? */
@Service
public class PdfExportService {

    private final CrmQuoteMapper quoteMapper;
    private final CrmQuoteItemMapper itemMapper;
    private final CrmQuoteHistoryMapper historyMapper;
    // 1h PDF 缓存（T3.7）：部署阶段�?Redis（key=quote:pdf:{id}, ttl=3600s�?
            private final ConcurrentHashMap<Long, byte[]> pdfCache = new ConcurrentHashMap<>();
    private final long cacheTtlMillis = 3600_000L;
    private final ConcurrentHashMap<Long, Long> cacheTimestamp = new ConcurrentHashMap<>();

    @Autowired
    public PdfExportService(CrmQuoteMapper quoteMapper, CrmQuoteItemMapper itemMapper,
                            CrmQuoteHistoryMapper historyMapper) {
        this.quoteMapper = quoteMapper;
        this.itemMapper = itemMapper;
        this.historyMapper = historyMapper;
    }

    @AuditLog(module = "quote", action = "quote.pdf_download")
    public Result<byte[]> exportPdf(Long quoteId, Long operatorUserId) {
        CrmQuote quote = quoteMapper.selectById(quoteId);
        if (quote == null) return Result.fail(40401, "QUOTE_NOT_FOUND");
        List<CrmQuoteItem> items = itemMapper.selectByQuoteId(quoteId);

        // 1h 缓存 (V1.3.7 T3.7 + P2 修补 2: app.cache-ttl)
            byte[] cached = getCachedPdf(quoteId);
        if (cached != null) {
            recordPdfDownload(quoteId, operatorUserId, "CACHE_HIT " + cached.length + " bytes");
            return Result.ok(cached);
        }

        // 简化：构�?PDF 文本内容（部署阶段换 OpenPDF/iText�?
            StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n%昆山佰泰胜精密机械有限公司\n");
        sb.append("报价单号: ").append(quote.getQuoteNo()).append("\n");
        sb.append("客户: ").append(quote.getCustomerName()).append("\n");
        sb.append("金额: ¥").append(quote.getTotalAmount()).append(" ").append(quote.getCurrency()).append("\n");
        sb.append("交期: ").append(quote.getDeliveryDate()).append("\n");
        sb.append("状�? ").append(quote.getStatus()).append("\n");
        sb.append("审批签字: _______________\n");
        sb.append("审批时间: ").append(LocalDateTime.now()).append("\n");
        sb.append("明细:\n");
        sb.append("  客户图号 | 数量 | 单价 | 金额\n");
        for (CrmQuoteItem item : items) {
            sb.append("  ").append(displayCustomerDrawingNo(item))
              .append(" | 数量=").append(item.getQuantity())
              .append(" | 单价=").append(item.getUnitPrice())
              .append(" | 金额=").append(item.getAmount()).append("\n");
        }
        sb.append("%%EOF\n");
        byte[] bytes = sb.toString().getBytes();
        pdfCache.put(quoteId, bytes);  // 1h 缓存（部署阶段换 Redis�?
            cacheTimestamp.put(quoteId, System.currentTimeMillis());

        // 变更留痕 (V1.3.7 红线 5)
            recordPdfDownload(quoteId, operatorUserId, "FRESH " + bytes.length + " bytes");
        return Result.ok(bytes);
    }

    @AuditLog(module = "quote", action = "quote.excel_download")
    public Result<byte[]> exportExcel(Long quoteId, Long operatorUserId) {
        CrmQuote quote = quoteMapper.selectById(quoteId);
        if (quote == null) return Result.fail(40401, "QUOTE_NOT_FOUND");
        List<CrmQuoteItem> items = itemMapper.selectByQuoteId(quoteId);

        // 简化：构�?Excel 文本内容（部署阶段换 POI�?
            StringBuilder sb = new StringBuilder();
        sb.append("Sheet1: 基本信息\n");
        sb.append("报价单号,客户,金额,币种,交期,状态\n");
        sb.append(quote.getQuoteNo()).append(",")
          .append(quote.getCustomerName()).append(",")
          .append(quote.getTotalAmount()).append(",")
          .append(quote.getCurrency()).append(",")
          .append(quote.getDeliveryDate()).append(",")
          .append(quote.getStatus()).append("\n");
        sb.append("Sheet2: 报价明细\n");
        sb.append("客户图号,内部图号,材质,数量,单价,金额,是否FA,是否新件\n");
        for (CrmQuoteItem item : items) {
            sb.append(displayCustomerDrawingNo(item)).append(",")
              .append(item.getDrawingNo() != null ? item.getDrawingNo() : "").append(",")
              .append(item.getMaterial()).append(",")
              .append(item.getQuantity()).append(",")
              .append(item.getUnitPrice()).append(",")
              .append(item.getAmount()).append(",")
              .append(item.getIsFa()).append(",")
              .append(item.getIsNew()).append("\n");
        }
        return Result.ok(sb.toString().getBytes());
    }

    private static String displayCustomerDrawingNo(CrmQuoteItem item) {
        if (item.getCustomerDrawingNo() != null && !item.getCustomerDrawingNo().isBlank()) {
            return item.getCustomerDrawingNo().trim();
        }
        return item.getDrawingNo() != null ? item.getDrawingNo() : "—";
    }

    /**
     * PDF 缓存（带 1h TTL 校验）
     */
    private byte[] getCachedPdf(Long quoteId) {
        Long ts = cacheTimestamp.get(quoteId);
        if (ts == null) return null;
        if (System.currentTimeMillis() - ts > cacheTtlMillis) {
            pdfCache.remove(quoteId);
            cacheTimestamp.remove(quoteId);
            return null;
        }
        return pdfCache.get(quoteId);
    }

    /**
     * PDF 下载审计留痕（V1.3.7 红线 5：变更留痕）
     */
    private void recordPdfDownload(Long quoteId, Long operatorUserId, String sizeInfo) {
        CrmQuoteHistory h = new CrmQuoteHistory();
        h.setQuoteId(quoteId);
        h.setOperation("PDF_DOWNLOAD");
        h.setChangedBy(operatorUserId);
        h.setChangedAt(LocalDateTime.now());
        h.setAfterJson(sizeInfo);
        historyMapper.insert(h);
    }
}
