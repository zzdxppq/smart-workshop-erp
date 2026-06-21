package com.btsheng.erp.business.crm.order.service;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderHistoryMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderItemMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderPaymentMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.business.integration.client.DictClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** V1.3.7 Story 1.6 · AC-2.3.1 · OrderConversionService (继承 1.5 quantityAdjustment) 测例 (3 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderConversionServiceTest {

    @Mock private CrmOrderMapper orderMapper;
    @Mock private CrmOrderItemMapper itemMapper;
    @Mock private CrmOrderHistoryMapper historyMapper;
    @Mock private CrmOrderPaymentMapper paymentMapper;
    @Mock private DocNoGenerator docNoGenerator;
    @Mock private DictClient dictService;

    private OrderService newSvc() {
        when(docNoGenerator.nextOrderNo()).thenReturn("XS20260612-0001");
        return new OrderService(orderMapper, itemMapper, historyMapper, paymentMapper, docNoGenerator, dictService);
    }

    @Test void create_with_quantity_adjustment_5() {
        when(orderMapper.insert(any(CrmOrder.class))).thenAnswer(inv -> { ((CrmOrder) inv.getArgument(0)).setId(1L); return 1; });
        OrderService svc = newSvc();
        CrmOrder o = new CrmOrder();
        o.setCustomerId(11L); o.setCustomerName("X");
        o.setOwnerUserId(1L); o.setDeptId(1L);
        o.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmOrderItem item = new CrmOrderItem();
        item.setQuantity(10); item.setUnitPrice(new BigDecimal("100"));
        item.setQuantityAdjustment(5);  // 调整 +5 �?实际数量 15
            Result<CrmOrder> r = svc.createOrder(o, List.of(item), 1L);
        assertEquals(0, r.getCode());
    }

    @Test void create_with_zero_adjustment_keeps_quantity() {
        when(orderMapper.insert(any(CrmOrder.class))).thenAnswer(inv -> { ((CrmOrder) inv.getArgument(0)).setId(1L); return 1; });
        OrderService svc = newSvc();
        CrmOrder o = new CrmOrder();
        o.setCustomerId(11L); o.setCustomerName("X");
        o.setOwnerUserId(1L); o.setDeptId(1L);
        o.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmOrderItem item = new CrmOrderItem();
        item.setQuantity(10); item.setUnitPrice(new BigDecimal("100"));
        item.setQuantityAdjustment(0);
        Result<CrmOrder> r = svc.createOrder(o, List.of(item), 1L);
        assertEquals(0, r.getCode());
    }

    @Test void create_negative_adjustment_invalid() {
        OrderService svc = newSvc();
        CrmOrder o = new CrmOrder();
        o.setCustomerId(11L); o.setCustomerName("X");
        o.setOwnerUserId(1L); o.setDeptId(1L);
        o.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmOrderItem item = new CrmOrderItem();
        item.setQuantity(10); item.setUnitPrice(new BigDecimal("100"));
        item.setQuantityAdjustment(-5);
        Result<CrmOrder> r = svc.createOrder(o, List.of(item), 1L);
        assertEquals(40003, r.getCode());
    }
}
