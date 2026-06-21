package com.btsheng.erp.business.crm.order.service;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderHistoryMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderItemMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** V1.3.7 Story 1.6 · OrderProfitService 测例 (3 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderProfitServiceTest {

    @Mock private CrmOrderMapper orderMapper;
    @Mock private CrmOrderItemMapper itemMapper;
    @Mock private CrmOrderHistoryMapper historyMapper;

    private OrderProfitService newSvc() {
        return new OrderProfitService(orderMapper, itemMapper, historyMapper);
    }

    @Test void profit_positive() {
        CrmOrder o = new CrmOrder();
        o.setId(1L); o.setOrderNo("XS-001");
        o.setTotalAmount(new BigDecimal("100000"));
        o.setProductionOrderNo("GD20260612-0001");  // 有生产
            when(orderMapper.selectById(1L)).thenReturn(o);
        when(itemMapper.selectByOrderId(1L)).thenReturn(Collections.emptyList());
        Result<Map<String, Object>> r = newSvc().analyzeProfit(1L, 1L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().get("profit"));
        assertEquals(false, r.getData().get("isLoss"));
    }

    @Test void profit_pdf_export() {
        CrmOrder o = new CrmOrder();
        o.setId(1L); o.setOrderNo("XS-001");
        o.setTotalAmount(new BigDecimal("10000"));
        when(orderMapper.selectById(1L)).thenReturn(o);
        when(itemMapper.selectByOrderId(1L)).thenReturn(Collections.emptyList());
        Result<byte[]> r = newSvc().exportProfitPdf(1L, 1L);
        assertEquals(0, r.getCode());
        String content = new String(r.getData());
        assertTrue(content.contains("订单号"));
        assertTrue(content.contains("利润率"));
    }

    @Test void profit_analysis_not_found() {
        when(orderMapper.selectById(99L)).thenReturn(null);
        Result<Map<String, Object>> r = newSvc().analyzeProfit(99L, 1L);
        assertEquals(40401, r.getCode());
    }
}
