package com.btsheng.erp.business.crm.order.service;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderHistoryMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderItemMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderPaymentMapper;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** V1.3.7 Story 1.6 · AC-2.3.4 · OrderPdfExportService 测例 (3 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderPdfExportServiceTest {

    @Mock private CrmOrderMapper orderMapper;
    @Mock private CrmOrderItemMapper itemMapper;
    @Mock private CrmOrderHistoryMapper historyMapper;
    @Mock private CrmOrderPaymentMapper paymentMapper;

    private OrderPdfExportService newSvc() {
        OrderService orderService = mock(OrderService.class);
        return new OrderPdfExportService(orderMapper, itemMapper, historyMapper, paymentMapper, orderService);
    }

    @Test void export_pdf_success() {
        CrmOrder o = new CrmOrder();
        o.setId(1L); o.setOrderNo("XS20260612-0001");
        o.setCustomerName("X"); o.setTotalAmount(new BigDecimal("1000"));
        o.setStatus("CONFIRMED");
        when(orderMapper.selectById(1L)).thenReturn(o);
        when(itemMapper.selectByOrderId(1L)).thenReturn(Collections.emptyList());
        Result<byte[]> r = newSvc().exportPdf(1L, 1L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData());
        assertTrue(new String(r.getData()).contains("XS20260612-0001"));
    }

    @Test void export_pdf_cache_hit_second_call() {
        CrmOrder o = new CrmOrder();
        o.setId(1L); o.setOrderNo("XS20260612-0001");
        o.setCustomerName("X"); o.setTotalAmount(new BigDecimal("1000"));
        o.setStatus("CONFIRMED");
        when(orderMapper.selectById(1L)).thenReturn(o);
        when(itemMapper.selectByOrderId(1L)).thenReturn(Collections.emptyList());
        OrderPdfExportService svc = newSvc();
        Result<byte[]> r1 = svc.exportPdf(1L, 1L);
        Result<byte[]> r2 = svc.exportPdf(1L, 1L);
        assertEquals(0, r1.getCode());
        assertEquals(0, r2.getCode());
        // 第二次命中缓存，bytes 长度应一致
            assertEquals(r1.getData().length, r2.getData().length);
    }

    @Test void export_excel_multi_sheet() {
        CrmOrder o = new CrmOrder();
        o.setId(1L); o.setOrderNo("XS20260612-0001");
        o.setCustomerName("X"); o.setTotalAmount(new BigDecimal("1000"));
        o.setStatus("SHIPPED");
        CrmOrderItem item = new CrmOrderItem();
        item.setDrawingNo("DWG-001"); item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("100")); item.setAmount(new BigDecimal("1000"));
        when(orderMapper.selectById(1L)).thenReturn(o);
        when(itemMapper.selectByOrderId(1L)).thenReturn(List.of(item));
        when(orderMapper.selectById(1L)).thenReturn(o);
        Result<byte[]> r = newSvc().exportExcel(1L, 1L);
        assertEquals(0, r.getCode());
        String content = new String(r.getData());
        assertTrue(content.contains("Sheet1"));
        assertTrue(content.contains("Sheet2"));
        assertTrue(content.contains("Sheet3"));
        assertTrue(content.contains("Sheet4"));
    }
}
