package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteHistoryMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.business.integration.client.DictClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** V1.3.7 Story 1.5 · AC-2.2.1 · QuoteService 测例 (8 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuoteServiceTest {

    @Mock private CrmQuoteMapper quoteMapper;
    @Mock private CrmQuoteItemMapper itemMapper;
    @Mock private CrmQuoteHistoryMapper historyMapper;
    @Mock private DictClient dictService;

    @Test void create_quote_success() {
        when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(new ArrayList<>()));
        when(quoteMapper.insert(any(CrmQuote.class))).thenAnswer(inv -> { ((CrmQuote) inv.getArgument(0)).setId(1L); return 1; });
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        CrmQuote q = new CrmQuote();
        q.setCustomerId(11L);
        q.setCustomerName("上海某精密");
        q.setOwnerUserId(10086L);
        q.setDeptId(1L);
        q.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmQuoteItem item = new CrmQuoteItem();
        item.setDrawingNo("DWG-001");
        item.setMaterial("STEEL");
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("5000"));
        Result<CrmQuote> r = svc.createQuote(q, List.of(item), 10086L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().getQuoteNo());
        assertTrue(r.getData().getQuoteNo().startsWith("BJ"));
    }

    @Test void create_quote_no_items() {
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        CrmQuote q = new CrmQuote();
        q.setCustomerId(11L);
        Result<CrmQuote> r = svc.createQuote(q, List.of(), 1L);
        assertEquals(40001, r.getCode());
        assertTrue(r.getMessage().contains("ITEMS_EMPTY"));
    }

    @Test void create_quote_blacklist_rejected() {
        Dict black = new Dict();
        black.setDictCode("C0011-BL");
        when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(List.of(black)));
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        CrmQuote q = new CrmQuote();
        q.setCustomerId(11L);
        CrmQuoteItem item = new CrmQuoteItem();
        item.setQuantity(1); item.setUnitPrice(BigDecimal.ONE);
        Result<CrmQuote> r = svc.createQuote(q, List.of(item), 1L);
        assertEquals(40902, r.getCode());
    }

    @Test void create_quote_amount_auto_calc() {
        when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(new ArrayList<>()));
        when(quoteMapper.insert(any(CrmQuote.class))).thenAnswer(inv -> { ((CrmQuote) inv.getArgument(0)).setId(1L); return 1; });
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        CrmQuote q = new CrmQuote();
        q.setCustomerId(11L); q.setCustomerName("X"); q.setOwnerUserId(1L); q.setDeptId(1L);
        q.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmQuoteItem i1 = new CrmQuoteItem(); i1.setQuantity(10); i1.setUnitPrice(new BigDecimal("100"));
        CrmQuoteItem i2 = new CrmQuoteItem(); i2.setQuantity(5); i2.setUnitPrice(new BigDecimal("200"));
        Result<CrmQuote> r = svc.createQuote(q, List.of(i1, i2), 1L);
        assertEquals(new BigDecimal("2000"), r.getData().getTotalAmount());
    }

    @Test void update_quote_submitted_rejected() {
        CrmQuote existing = new CrmQuote();
        existing.setId(1L);
        existing.setStatus("SUBMITTED");
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        CrmQuote q = new CrmQuote();
        q.setCustomerName("Updated");
        Result<CrmQuote> r = svc.updateQuote(1L, q, 1L);
        assertEquals(40903, r.getCode());
    }

    @Test void update_quote_draft_success() {
        CrmQuote existing = new CrmQuote();
        existing.setId(1L);
        existing.setStatus("DRAFT");
        existing.setCustomerName("Old");
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        CrmQuote q = new CrmQuote();
        q.setCustomerName("New");
        q.setComment("updated");
        Result<CrmQuote> r = svc.updateQuote(1L, q, 1L);
        assertEquals(0, r.getCode());
        assertEquals("New", r.getData().getCustomerName());
    }

    @Test void get_quote_with_history() {
        CrmQuote q = new CrmQuote(); q.setId(1L);
        when(quoteMapper.selectById(1L)).thenReturn(q);
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        Result<java.util.Map<String, Object>> r = svc.getQuoteWithHistory(1L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().get("quote"));
    }

    @Test void delete_quote_soft() {
        CrmQuote existing = new CrmQuote(); existing.setId(1L); existing.setIsDeleted(0);
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        Result<Void> r = svc.deleteQuote(1L, 1L);
        assertEquals(0, r.getCode());
    }
}
