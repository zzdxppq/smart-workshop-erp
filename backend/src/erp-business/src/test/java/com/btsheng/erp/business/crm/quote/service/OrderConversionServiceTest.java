package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteHistoryMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** V1.3.7 Story 1.5 · AC-2.2.3 · OrderConversionService 测例 (3 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderConversionServiceTest {

    @Mock private CrmQuoteMapper quoteMapper;
    @Mock private CrmQuoteItemMapper itemMapper;
    @Mock private CrmQuoteHistoryMapper historyMapper;

    @Test void convert_quote_to_order_success() {
        CrmQuote existing = new CrmQuote(); existing.setId(1L); existing.setStatus("APPROVED");
        existing.setQuoteNo("BJ20260611-0001");
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        when(itemMapper.selectByQuoteId(1L)).thenReturn(java.util.List.of());
        OrderConversionService svc = new OrderConversionService(quoteMapper, itemMapper, mock(QuoteService.class));
        Result<Map<String, Object>> r = svc.convertToOrder(1L, 1L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().get("orderNo"));
        assertTrue(r.getData().get("orderNo").toString().startsWith("XS"));
    }

    @Test void convert_rejected_quote_invalid_state() {
        CrmQuote existing = new CrmQuote(); existing.setId(1L); existing.setStatus("REJECTED");
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        OrderConversionService svc = new OrderConversionService(quoteMapper, itemMapper, mock(QuoteService.class));
        Result<Map<String, Object>> r = svc.convertToOrder(1L, 1L);
        assertEquals(40904, r.getCode());
    }

    @Test void convert_draft_quote_invalid_state() {
        CrmQuote existing = new CrmQuote(); existing.setId(1L); existing.setStatus("DRAFT");
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        OrderConversionService svc = new OrderConversionService(quoteMapper, itemMapper, mock(QuoteService.class));
        Result<Map<String, Object>> r = svc.convertToOrder(1L, 1L);
        assertEquals(40904, r.getCode());
    }
}
