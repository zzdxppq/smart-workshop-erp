package com.btsheng.erp.business.crm.order.service;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderHistoryMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderItemMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderPaymentMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
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

/** V1.3.7 Story 1.6 · AC-2.3.1/2/3/4 · OrderService 单元测例 (8 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

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

    @Test void create_order_success() {
        when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(new ArrayList<>()));
        when(dictService.listByType("CREDIT_LIMIT")).thenReturn(Result.ok(new ArrayList<>()));
        when(orderMapper.insert(any(CrmOrder.class))).thenAnswer(inv -> { ((CrmOrder) inv.getArgument(0)).setId(1L); return 1; });
        OrderService svc = newSvc();
        CrmOrder o = new CrmOrder();
        o.setCustomerId(11L); o.setCustomerName("X");
        o.setOwnerUserId(1L); o.setDeptId(1L);
        o.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmOrderItem item = new CrmOrderItem();
        item.setQuantity(10); item.setUnitPrice(new BigDecimal("100"));
        Result<CrmOrder> r = svc.createOrder(o, List.of(item), 1L);
        assertEquals(0, r.getCode());
        assertEquals("XS20260612-0001", r.getData().getOrderNo());
    }

    @Test void get_order_not_found() {
        when(orderMapper.selectById(99L)).thenReturn(null);
        OrderService svc = newSvc();
        Result<CrmOrder> r = svc.getOrder(99L);
        assertEquals(40401, r.getCode());
    }

    @Test void update_order_draft_ok() {
        CrmOrder existing = new CrmOrder();
        existing.setId(1L); existing.setStatus("DRAFT");
        when(orderMapper.selectById(1L)).thenReturn(existing);
        OrderService svc = newSvc();
        CrmOrder upd = new CrmOrder();
        upd.setCustomerName("Updated");
        Result<CrmOrder> r = svc.updateOrder(1L, upd, null, 1L);
        assertEquals(0, r.getCode());
    }

    @Test void update_order_confirmed_invalid() {
        CrmOrder existing = new CrmOrder();
        existing.setId(1L); existing.setStatus("CONFIRMED");
        when(orderMapper.selectById(1L)).thenReturn(existing);
        OrderService svc = newSvc();
        CrmOrder upd = new CrmOrder();
        Result<CrmOrder> r = svc.updateOrder(1L, upd, null, 1L);
        assertEquals(40903, r.getCode());
    }

    @Test void list_orders_by_role() {
        when(orderMapper.selectList(any())).thenReturn(new ArrayList<>());
        OrderService svc = newSvc();
        Result<List<CrmOrder>> r = svc.listOrders(1, 20, "DRAFT", null, 1L, 1L, "salesperson");
        assertEquals(0, r.getCode());
    }

    @Test void delete_order_soft() {
        CrmOrder existing = new CrmOrder();
        existing.setId(1L); existing.setStatus("DRAFT");
        when(orderMapper.selectById(1L)).thenReturn(existing);
        OrderService svc = newSvc();
        Result<Void> r = svc.deleteOrder(1L, 1L);
        assertEquals(0, r.getCode());
        verify(orderMapper).updateById((CrmOrder) any());
    }

    @Test void confirm_order_draft_to_confirmed() {
        CrmOrder existing = new CrmOrder();
        existing.setId(1L); existing.setStatus("DRAFT");
        when(orderMapper.selectById(1L)).thenReturn(existing);
        OrderService svc = newSvc();
        Result<CrmOrder> r = svc.confirmOrder(1L, 1L);
        assertEquals(0, r.getCode());
        assertEquals("CONFIRMED", r.getData().getStatus());
    }

    @Test void ship_order_producing() {
        CrmOrder existing = new CrmOrder();
        existing.setId(1L); existing.setStatus("PRODUCING");
        when(orderMapper.selectById(1L)).thenReturn(existing);
        CrmOrderItem item = new CrmOrderItem();
        item.setId(1L); item.setQuantity(10); item.setShippedQty(0);
        when(itemMapper.selectByOrderId(1L)).thenReturn(List.of(item));
        OrderService svc = newSvc();
        Result<CrmOrder> r = svc.ship(1L, 1L);
        assertEquals(0, r.getCode());
        assertEquals("SHIPPED", r.getData().getStatus());
    }
}
