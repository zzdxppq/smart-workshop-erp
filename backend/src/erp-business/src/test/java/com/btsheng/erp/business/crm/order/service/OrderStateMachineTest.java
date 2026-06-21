package com.btsheng.erp.business.crm.order.service;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** V1.3.7 Story 1.6 · AC-2.3.2 · 7 状态机测例 (5 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderStateMachineTest {

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

    private CrmOrder mockOrder(String status) {
        CrmOrder o = new CrmOrder();
        o.setId(1L); o.setStatus(status);
        when(orderMapper.selectById(1L)).thenReturn(o);
        return o;
    }

    @Test void confirm_draft_to_confirmed_legal() {
        mockOrder("DRAFT");
        Result<CrmOrder> r = newSvc().confirmOrder(1L, 1L);
        assertEquals(0, r.getCode());
        assertEquals("CONFIRMED", r.getData().getStatus());
    }

    @Test void confirm_producing_invalid_state() {
        mockOrder("PRODUCING");
        Result<CrmOrder> r = newSvc().confirmOrder(1L, 1L);
        assertEquals(40904, r.getCode());
    }

    @Test void approve_confirmed_to_producing_legal() {
        mockOrder("CONFIRMED");
        Result<CrmOrder> r = newSvc().approveOrder(1L, 1L);
        assertEquals(0, r.getCode());
        assertEquals("PRODUCING", r.getData().getStatus());
    }

    @Test void reject_confirmed_to_draft_legal() {
        mockOrder("CONFIRMED");
        Result<CrmOrder> r = newSvc().rejectOrder(1L, "价格过高", 1L);
        assertEquals(0, r.getCode());
        assertEquals("DRAFT", r.getData().getStatus());
    }

    @Test void close_settled_to_closed_legal() {
        mockOrder("SETTLED");
        Result<CrmOrder> r = newSvc().closeOrder(1L, 1L);
        assertEquals(0, r.getCode());
        assertEquals("CLOSED", r.getData().getStatus());
    }
}
