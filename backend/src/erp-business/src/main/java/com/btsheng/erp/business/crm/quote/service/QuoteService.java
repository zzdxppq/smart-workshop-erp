package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteHistory;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteHistoryMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.business.crm.sales.entity.CrmCustomer;
import com.btsheng.erp.business.crm.sales.mapper.CrmCustomerMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.business.integration.client.DictClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ?? Service?V1.3.7 ? Story 1.5 ? AC-2.2.1?? *
 * ?? Story 1.3 DocNoGenerator (story-1.5-id T1.4) + Story 1.3 sys_dict ???? * + 8 ???????
 */
@Service
public class QuoteService {

    private final CrmQuoteMapper quoteMapper;
    private final CrmQuoteItemMapper itemMapper;
    private final CrmQuoteHistoryMapper historyMapper;
    private final DictClient dictClient;
    private final CrmCustomerMapper customerMapper;
            private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicLong seqCounter = new AtomicLong(1);

    @Autowired
    public QuoteService(CrmQuoteMapper quoteMapper, CrmQuoteItemMapper itemMapper,
                       CrmQuoteHistoryMapper historyMapper, DictClient dictClient,
                       CrmCustomerMapper customerMapper) {
        this.quoteMapper = quoteMapper;
        this.itemMapper = itemMapper;
        this.historyMapper = historyMapper;
        this.dictClient = dictClient;
        this.customerMapper = customerMapper;
    }

    @Transactional
    @AuditLog(module = "quote", action = "quote.create")
    public Result<CrmQuote> createQuote(CrmQuote quote, List<CrmQuoteItem> items, Long operatorUserId) {
        // 1. ????
            if (items == null || items.isEmpty()) {
            return Result.fail(40001, "QUOTE_ITEMS_EMPTY");
        }
        if (quote.getDeliveryDate() != null && quote.getDeliveryDate().isBefore(LocalDate.now())) {
            return Result.fail(40001, "QUOTE_DELIVERY_DATE_INVALID");
        }

        // 2. 黑名单校验（V1.3.7 P1 修补 · 字典服务不可用时跳过）
            Result<List<Dict>> dicts = null;
        try {
            dicts = dictClient.listByType("CUSTOMER_STATUS");
        } catch (Exception e) {
            // erp-platform 未启动时不阻断报价创建
        }
        if (dicts != null && dicts.getCode() == 0 && dicts.getData() != null) {
            String customerCode = "C" + String.format("%04d", quote.getCustomerId()) + "-BL";
            boolean isBlacklisted = dicts.getData().stream()
                .anyMatch(d -> customerCode.equals(d.getDictCode()));
            if (isBlacklisted) {
                return Result.fail(40902, "CUSTOMER_BLACKLIST");
            }
        }

        // 3. 计算总金额（业务员阶段单价可为空）
            BigDecimal total = items.stream()
            .map(item -> {
                BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                return price.multiply(BigDecimal.valueOf(qty));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        quote.setTotalAmount(total);

        // 4. ??????(Story 1.3 DocNoGenerator ?? BJ{yyyyMMdd}{seq:4})
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = seqCounter.getAndIncrement();
        String quoteNo = String.format("BJ%s%04d", date, seq);
        quote.setQuoteNo(quoteNo);
        quote.setStatus("DRAFT");

        // 4.1 回填客户名称（V1.3.9 P0：crm_customer.name → customer_name）
        if (quote.getCustomerId() != null && (quote.getCustomerName() == null || quote.getCustomerName().isBlank())) {
            CrmCustomer customer = customerMapper.selectById(quote.getCustomerId());
            if (customer != null) {
                quote.setCustomerName(customer.getName());
            }
        }

        // 4.2 兜底 dept_id（V1.3.9 P0：crm_quote.dept_id 无默认值）
        if (quote.getDeptId() == null) {
            quote.setDeptId(0L);
        }

        // 4.3 兜底 delivery_date（V1.3.9 P0：QuoteForm.vue 未设交货日期字段，表为 NOT NULL）
        if (quote.getDeliveryDate() == null) {
            quote.setDeliveryDate(LocalDate.now().plusDays(30)); // 默认 30 天后交货
        }

        quoteMapper.insert(quote);
        for (CrmQuoteItem item : items) {
            item.setQuoteId(quote.getId());
            // V1.3.9 P0：兜底 NOT NULL 字段
            if (item.getMaterial() == null || item.getMaterial().isBlank()) {
                item.setMaterial("-");
            }
            if (item.getDrawingNo() == null || item.getDrawingNo().isBlank()) {
                item.setDrawingNo("-");
            }
            if (item.getSort() == null) {
                item.setSort(0);
            }
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            item.setUnitPrice(price);
            item.setAmount(price.multiply(BigDecimal.valueOf(qty)));
            itemMapper.insert(item);
        }

        // 5. ???? (V1.3.7 ?? 5)
            recordHistory(quote.getId(), "CREATE", null, quote, operatorUserId);
        return Result.ok(quote);
    }

    @AuditLog(module = "quote", action = "quote.update")
    public Result<CrmQuote> updateQuote(Long id, CrmQuote quote, Long operatorUserId) {
        CrmQuote existing = quoteMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "QUOTE_NOT_FOUND");
        if (!"DRAFT".equals(existing.getStatus())) {
            return Result.fail(40903, "QUOTE_NOT_EDITABLE");
        }
        CrmQuote before = clone(existing);
        existing.setCustomerId(quote.getCustomerId());
        existing.setCustomerName(quote.getCustomerName());
        existing.setDeliveryDate(quote.getDeliveryDate());
        existing.setIsFa(quote.getIsFa());
        existing.setIsNew(quote.getIsNew());
        existing.setComment(quote.getComment());
        quoteMapper.updateById(existing);
        recordHistory(id, "UPDATE", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    @Transactional
    @AuditLog(module = "quote", action = "quote.save_draft")
    public Result<CrmQuote> saveDraftWithItems(Long id, CrmQuote quote, List<CrmQuoteItem> items, Long operatorUserId) {
        CrmQuote existing = quoteMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "QUOTE_NOT_FOUND");
        if (!"DRAFT".equals(existing.getStatus())) {
            return Result.fail(40903, "QUOTE_NOT_EDITABLE");
        }
        if (items == null || items.isEmpty()) {
            return Result.fail(40001, "QUOTE_ITEMS_EMPTY");
        }
        CrmQuote before = clone(existing);
        existing.setCustomerId(quote.getCustomerId());
        existing.setCustomerName(quote.getCustomerName());
        existing.setIsFa(quote.getIsFa());
        existing.setIsNew(quote.getIsNew());
        existing.setComment(quote.getComment());
        existing.setDeliveryDate(null);

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CrmQuoteItem> del =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        del.eq("quote_id", id);
        itemMapper.delete(del);

        BigDecimal total = BigDecimal.ZERO;
        int sort = 0;
        for (CrmQuoteItem item : items) {
            item.setId(null);
            item.setQuoteId(id);
            item.setSort(sort++);
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            item.setUnitPrice(price);
            item.setAmount(price.multiply(BigDecimal.valueOf(qty)));
            total = total.add(item.getAmount());
            itemMapper.insert(item);
        }
        existing.setTotalAmount(total);
        quoteMapper.updateById(existing);
        recordHistory(id, "UPDATE", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    public Result<CrmQuote> getQuote(Long id) {
        CrmQuote q = quoteMapper.selectById(id);
        if (q == null) return Result.fail(40401, "QUOTE_NOT_FOUND");
        return Result.ok(q);
    }

    public Result<Map<String, Object>> getQuoteWithHistory(Long id) {
        CrmQuote q = quoteMapper.selectById(id);
        if (q == null) return Result.fail(40401, "QUOTE_NOT_FOUND");
        List<CrmQuoteItem> items = itemMapper.selectByQuoteId(id);
        List<CrmQuoteHistory> history = historyMapper.selectByQuoteId(id);
        Map<String, Object> result = new HashMap<>();
        result.put("quote", q);
        result.put("items", items);
        result.put("history", history);
        return Result.ok(result);
    }

    public Result<List<CrmQuote>> listQuotes(int pageNum, int pageSize, String status, Long customerId,
                                           Long ownerUserId, Long deptId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CrmQuote> qw =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (status != null) qw.eq("status", status);
        if (customerId != null) qw.eq("customer_id", customerId);
        if (ownerUserId != null) qw.eq("owner_user_id", ownerUserId);
        if (deptId != null) qw.eq("dept_id", deptId);
        qw.eq("is_deleted", 0);
        qw.orderByDesc("created_at");
        qw.last("LIMIT " + (pageNum * pageSize) + " OFFSET " + (pageNum * pageSize - pageSize));
        return Result.ok(quoteMapper.selectList(qw));
    }

    @Transactional
    @AuditLog(module = "quote", action = "quote.delete")
    public Result<Void> deleteQuote(Long id, Long operatorUserId) {
        CrmQuote existing = quoteMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "QUOTE_NOT_FOUND");
        CrmQuote before = clone(existing);
        existing.setIsDeleted(1);
        quoteMapper.updateById(existing);
        recordHistory(id, "DELETE", before, existing, operatorUserId);
        return Result.ok();
    }

    public void recordHistory(Long quoteId, String operation, CrmQuote before, CrmQuote after, Long userId) {
        CrmQuoteHistory h = new CrmQuoteHistory();
        h.setQuoteId(quoteId);
        h.setOperation(operation);
        h.setChangedBy(userId);
        h.setChangedAt(LocalDateTime.now());
        try {
            h.setBeforeJson(before == null ? null : mapper.writeValueAsString(before));
            h.setAfterJson(after == null ? null : mapper.writeValueAsString(after));
        } catch (Exception e) {
            // ignore serialization error
        }
        historyMapper.insert(h);
    }

    private CrmQuote clone(CrmQuote q) {
        try { return mapper.readValue(mapper.writeValueAsString(q), CrmQuote.class); }
        catch (Exception e) { return q; }
    }
}
