package com.btsheng.erp.business.crm.order.service;

import com.btsheng.erp.business.crm.order.dto.OrderCancelRequest;
import com.btsheng.erp.business.crm.order.dto.OrderConfirmRequest;
import com.btsheng.erp.business.crm.order.dto.OrderCreateRequest;
import com.btsheng.erp.business.crm.order.dto.OrderUpdateRequest;
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

/** V1.3.7 Story 1.6 · AC-2.3.1/2 · OrderController 集成测例 (4 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderControllerIntegrationTest {

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

    @Test void crud_lifecycle() {
        when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(List.of()));
        when(orderMapper.insert(any(CrmOrder.class))).thenAnswer(inv -> { ((CrmOrder) inv.getArgument(0)).setId(1L); return 1; });
        CrmOrder existing = new CrmOrder();
        existing.setId(1L); existing.setStatus("DRAFT");
        when(orderMapper.selectById(1L)).thenReturn(existing);
        OrderService svc = newSvc();

        OrderCreateRequest req = new OrderCreateRequest();
        CrmOrder o = new CrmOrder();
        o.setCustomerId(11L); o.setCustomerName("X");
        o.setOwnerUserId(1L); o.setDeptId(1L);
        o.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmOrderItem item = new CrmOrderItem();
        item.setQuantity(10); item.setUnitPrice(new BigDecimal("100"));
        req.setOrder(o);
        req.setItems(List.of(item));
        Result<CrmOrder> cr = svc.createOrder(req.getOrder(), req.getItems(), 1L);
        assertEquals(0, cr.getCode());

        // confirm
            Result<CrmOrder> cf = svc.confirmOrder(1L, 1L);
        assertEquals(0, cf.getCode());

        // cancel (need to reset state to DRAFT first)
            existing.setStatus("DRAFT");
        OrderCancelRequest cancel = new OrderCancelRequest();
        cancel.setReason("客户取消");
        Result<CrmOrder> cc = svc.cancelOrder(1L, cancel.getReason(), 1L);
        assertEquals(0, cc.getCode());
        assertEquals("CANCELLED", cc.getData().getStatus());
    }

    @Test void confirm_then_ship_state_machine() {
        CrmOrder existing = new CrmOrder();
        existing.setId(1L); existing.setStatus("DRAFT");
        when(orderMapper.selectById(1L)).thenReturn(existing);
        OrderService svc = newSvc();

        // confirm
            Result<CrmOrder> cf = svc.confirmOrder(1L, 1L);
        assertEquals(0, cf.getCode());
        existing.setStatus("CONFIRMED");

        // approve -> PRODUCING
            Result<CrmOrder> ap = svc.approveOrder(1L, 1L);
        assertEquals(0, ap.getCode());
        existing.setStatus("PRODUCING");

        // ship
            CrmOrderItem item = new CrmOrderItem();
        item.setId(11L); item.setQuantity(10); item.setShippedQty(0);
        when(itemMapper.selectByOrderId(1L)).thenReturn(List.of(item));
        Result<CrmOrder> sh = svc.ship(1L, 1L);
        assertEquals(0, sh.getCode());
        assertEquals("SHIPPED", sh.getData().getStatus());
    }

    @Test void create_with_invalid_quantity_returns_40003() {
        OrderService svc = newSvc();
        CrmOrder o = new CrmOrder();
        o.setCustomerId(11L); o.setCustomerName("X");
        o.setOwnerUserId(1L); o.setDeptId(1L);
        o.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmOrderItem item = new CrmOrderItem();
        item.setQuantity(0);  // invalid
            item.setUnitPrice(new BigDecimal("100"));
        Result<CrmOrder> r = svc.createOrder(o, List.of(item), 1L);
        assertEquals(40003, r.getCode());
    }

    @Test void update_request_dto() {
        CrmOrder existing = new CrmOrder();
        existing.setId(1L); existing.setStatus("DRAFT");
        when(orderMapper.selectById(1L)).thenReturn(existing);
        OrderService svc = newSvc();
        OrderUpdateRequest req = new OrderUpdateRequest();
        CrmOrder o = new CrmOrder();
        o.setCustomerName("Updated");
        req.setOrder(o);
        Result<CrmOrder> r = svc.updateOrder(1L, req.getOrder(), req.getItems(), 1L);
        assertEquals(0, r.getCode());
    }
}
