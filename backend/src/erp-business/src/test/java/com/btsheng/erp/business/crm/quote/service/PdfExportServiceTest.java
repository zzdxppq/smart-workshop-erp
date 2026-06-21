package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** V2.1 · PDF 导出含客户图号 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PdfExportServiceTest {

    @Mock private CrmQuoteMapper quoteMapper;
    @Mock private CrmQuoteItemMapper itemMapper;
    @Mock private CrmQuoteHistoryMapper historyMapper;

    private PdfExportService service() {
        return new PdfExportService(quoteMapper, itemMapper, historyMapper);
    }

    @Test void exportPdf_prefers_customer_drawing_no() {
        CrmQuote quote = new CrmQuote();
        quote.setId(1L);
        quote.setQuoteNo("BJ202606210001");
        quote.setCustomerName("测试客户");
        quote.setTotalAmount(new BigDecimal("12000"));
        quote.setCurrency("CNY");
        quote.setStatus("APPROVED");
        when(quoteMapper.selectById(1L)).thenReturn(quote);

        CrmQuoteItem item = new CrmQuoteItem();
        item.setCustomerDrawingNo("615-03953-0009");
        item.setDrawingNo("DWG-20260621-0001");
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("1200"));
        item.setAmount(new BigDecimal("12000"));
        when(itemMapper.selectByQuoteId(1L)).thenReturn(List.of(item));

        Result<byte[]> r = service().exportPdf(1L, 1L);
        assertEquals(0, r.getCode());
        String text = new String(r.getData(), StandardCharsets.UTF_8);
        assertTrue(text.contains("615-03953-0009"), "PDF 应含客户图号");
        assertTrue(text.contains("客户图号"), "PDF 表头应标注客户图号");
    }

    @Test void exportExcel_includes_customer_drawing_column() {
        CrmQuote quote = new CrmQuote();
        quote.setId(2L);
        quote.setQuoteNo("BJ202606210002");
        quote.setCustomerName("测试客户");
        quote.setTotalAmount(new BigDecimal("5000"));
        quote.setCurrency("CNY");
        quote.setStatus("APPROVED");
        when(quoteMapper.selectById(2L)).thenReturn(quote);

        CrmQuoteItem item = new CrmQuoteItem();
        item.setCustomerDrawingNo("CUST-001");
        item.setDrawingNo("DWG-001");
        item.setMaterial("AL6061");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("5000"));
        item.setAmount(new BigDecimal("5000"));
        when(itemMapper.selectByQuoteId(2L)).thenReturn(List.of(item));

        Result<byte[]> r = service().exportExcel(2L, 1L);
        assertEquals(0, r.getCode());
        String csv = new String(r.getData(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("客户图号"));
        assertTrue(csv.contains("CUST-001"));
    }
}
