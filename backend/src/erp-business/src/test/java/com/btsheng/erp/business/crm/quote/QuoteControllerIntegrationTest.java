package com.btsheng.erp.business.crm.quote;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteHistory;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteHistoryMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.business.crm.quote.service.QuoteService;
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

/** V1.3.7 Story 1.5 · AC-2.2.1 · QuoteController 集成测例 (6 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuoteControllerIntegrationTest {

    @Mock private CrmQuoteMapper quoteMapper;
    @Mock private CrmQuoteItemMapper itemMapper;
    @Mock private CrmQuoteHistoryMapper historyMapper;
    @Mock private DictClient dictService;

    @Test void crud_full_lifecycle() {
        when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(new ArrayList<>()));
        when(quoteMapper.insert(any(CrmQuote.class))).thenAnswer(inv -> { ((CrmQuote) inv.getArgument(0)).setId(1L); return 1; });
        when(quoteMapper.selectById(1L)).thenAnswer(inv -> {
            CrmQuote q = new CrmQuote();
            q.setId(1L); q.setStatus("DRAFT");
            return q;
        });
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);

        CrmQuote q = new CrmQuote();
        q.setCustomerId(11L); q.setCustomerName("X");
        q.setOwnerUserId(1L); q.setDeptId(1L);
        q.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmQuoteItem item = new CrmQuoteItem();
        item.setQuantity(1); item.setUnitPrice(new BigDecimal("100"));

        Result<CrmQuote> cr = svc.createQuote(q, List.of(item), 1L);
        assertEquals(0, cr.getCode());
        assertNotNull(cr.getData().getQuoteNo());

        Result<CrmQuote> gr = svc.getQuote(1L);
        assertEquals(0, gr.getCode());

        // update
            CrmQuote upd = new CrmQuote();
        upd.setCustomerName("Updated");
        Result<CrmQuote> ur = svc.updateQuote(1L, upd, 1L);
        assertEquals(0, ur.getCode());

        // list
            Result<List<CrmQuote>> lr = svc.listQuotes(1, 20, null, null, null);
        assertEquals(0, lr.getCode());
    }

    @Test void create_quote_blacklist_rejected() {
        Dict bl = new Dict(); bl.setDictCode("C0011-BL");
        when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(List.of(bl)));
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        CrmQuote q = new CrmQuote();
        q.setCustomerId(11L);
        CrmQuoteItem item = new CrmQuoteItem();
        item.setQuantity(1); item.setUnitPrice(BigDecimal.ONE);
        Result<CrmQuote> r = svc.createQuote(q, List.of(item), 1L);
        assertEquals(40902, r.getCode());
    }

    @Test void create_quote_no_items() {
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        CrmQuote q = new CrmQuote();
        q.setCustomerId(11L);
        Result<CrmQuote> r = svc.createQuote(q, List.of(), 1L);
        assertEquals(40001, r.getCode());
    }

    @Test void update_submitted_quote() {
        CrmQuote existing = new CrmQuote();
        existing.setId(1L); existing.setStatus("SUBMITTED");
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        CrmQuote q = new CrmQuote();
        q.setCustomerName("X");
        Result<CrmQuote> r = svc.updateQuote(1L, q, 1L);
        assertEquals(40903, r.getCode());
    }

    @Test void list_with_6_filter() {
        when(quoteMapper.selectList(any())).thenReturn(new ArrayList<>());
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        Result<List<CrmQuote>> r = svc.listQuotes(1, 20, "DRAFT", 11L, 1L);
        assertEquals(0, r.getCode());
        verify(quoteMapper).selectList(any());
    }

    @Test void audit_log_persisted() {
        when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(new ArrayList<>()));
        when(quoteMapper.insert(any(CrmQuote.class))).thenAnswer(inv -> { ((CrmQuote) inv.getArgument(0)).setId(1L); return 1; });
        QuoteService svc = new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
        CrmQuote q = new CrmQuote();
        q.setCustomerId(11L); q.setCustomerName("X");
        q.setOwnerUserId(1L); q.setDeptId(1L);
        q.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmQuoteItem item = new CrmQuoteItem();
        item.setQuantity(1); item.setUnitPrice(new BigDecimal("100"));
        svc.createQuote(q, List.of(item), 100L);
        verify(historyMapper, atLeastOnce()).insert(any(CrmQuoteHistory.class));
    }
}
