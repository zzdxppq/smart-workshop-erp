package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 报价转订�?Service（V1.3.7 · Story 1.5 · AC-2.2.3�? *
 * 复用 Story 1.3 DocNoGenerator 模板 XS{yyyyMMdd}{seq:4}
 */
@Service
public class OrderConversionService {

    private final CrmQuoteMapper quoteMapper;
    private final CrmQuoteItemMapper itemMapper;
    private final QuoteService quoteService;
    private final AtomicLong orderSeq = new AtomicLong(1);

    @Autowired
    public OrderConversionService(CrmQuoteMapper quoteMapper, CrmQuoteItemMapper itemMapper,
                                 QuoteService quoteService) {
        this.quoteMapper = quoteMapper;
        this.itemMapper = itemMapper;
        this.quoteService = quoteService;
    }

    @AuditLog(module = "quote", action = "quote.convert_to_order")
    public Result<Map<String, Object>> convertToOrder(Long quoteId, Long operatorUserId) {
        CrmQuote quote = quoteMapper.selectById(quoteId);
        if (quote == null) return Result.fail(40401, "QUOTE_NOT_FOUND");
        if (!"APPROVED".equals(quote.getStatus())) {
            return Result.fail(40904, "QUOTE_STATE_INVALID");
        }

        // 1. 生成订单�?(Story 1.3 DocNoGenerator XS 模板)
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = orderSeq.getAndIncrement();
        String orderNo = String.format("XS%s%04d", date, seq);

        // 2. 自动带出明细
            List<CrmQuoteItem> items = itemMapper.selectByQuoteId(quoteId);

        // 3. 标记已转
            CrmQuote before = quote;
        quote.setStatus("CONVERTED");
        quoteMapper.updateById(quote);
        quoteService.recordHistory(quoteId, "CONVERT", before, quote, operatorUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("quoteId", quoteId);
        result.put("items", items);
        result.put("quoteNo", quote.getQuoteNo());
        return Result.ok(result);
    }
}
