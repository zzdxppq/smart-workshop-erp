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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** V1.3.7 Story 1.6 · AC-2.3.3 · 信用额度校验测例 (4 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCreditLimitTest {

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

    private Dict limit(String code, String label) {
        Dict d = new Dict();
        d.setDictType("CREDIT_LIMIT");
        d.setDictCode(code);
        d.setDictLabel(label);
        return d;
    }

    @Test void within_credit_limit_ok() {
        // 客户 C0012-LIMIT = 300000，下�?50000 �?OK
            when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(List.of()));
        when(dictService.listByType("CREDIT_LIMIT")).thenReturn(Result.ok(List.of(limit("C0012-LIMIT", "300000.00"))));
        when(orderMapper.insert(any(CrmOrder.class))).thenAnswer(inv -> { ((CrmOrder) inv.getArgument(0)).setId(1L); return 1; });
        OrderService svc = newSvc();
        CrmOrder o = new CrmOrder();
        o.setCustomerId(12L); o.setCustomerName("X");
        o.setOwnerUserId(1L); o.setDeptId(1L);
        o.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmOrderItem item = new CrmOrderItem();
        item.setQuantity(5); item.setUnitPrice(new BigDecimal("10000"));
        Result<CrmOrder> r = svc.createOrder(o, List.of(item), 1L);
        assertEquals(0, r.getCode());
    }

    @Test void exceeds_credit_limit_returns_40909() {
        // 客户 C0015-LIMIT = 100000，下�?500000 �?超限
            when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(List.of()));
        when(dictService.listByType("CREDIT_LIMIT")).thenReturn(Result.ok(List.of(limit("C0015-LIMIT", "100000.00"))));
        OrderService svc = newSvc();
        CrmOrder o = new CrmOrder();
        o.setCustomerId(15L); o.setCustomerName("X");
        o.setOwnerUserId(1L); o.setDeptId(1L);
        o.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmOrderItem item = new CrmOrderItem();
        item.setQuantity(5000); item.setUnitPrice(new BigDecimal("100"));
        Result<CrmOrder> r = svc.createOrder(o, List.of(item), 1L);
        assertEquals(40909, r.getCode());
    }

    @Test void blacklist_priority_over_credit_limit() {
        // 客户 C0001-BL 黑名�?+ 超信用额�?�?优先 40902
            Dict bl = new Dict();
        bl.setDictType("CUSTOMER_STATUS");
        bl.setDictCode("C0001-BL");
        when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(List.of(bl)));
        OrderService svc = newSvc();
        CrmOrder o = new CrmOrder();
        o.setCustomerId(1L); o.setCustomerName("X");
        o.setOwnerUserId(1L); o.setDeptId(1L);
        o.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmOrderItem item = new CrmOrderItem();
        item.setQuantity(5000); item.setUnitPrice(new BigDecimal("100"));
        Result<CrmOrder> r = svc.createOrder(o, List.of(item), 1L);
        assertEquals(40902, r.getCode());
    }

    @Test void unlimited_credit_limit_minus_one_passes() {
        // 客户 C0014-LIMIT = -1 无限�?
            when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(List.of()));
        when(dictService.listByType("CREDIT_LIMIT")).thenReturn(Result.ok(List.of(limit("C0014-LIMIT", "-1"))));
        when(orderMapper.insert(any(CrmOrder.class))).thenAnswer(inv -> { ((CrmOrder) inv.getArgument(0)).setId(1L); return 1; });
        OrderService svc = newSvc();
        CrmOrder o = new CrmOrder();
        o.setCustomerId(14L); o.setCustomerName("X");
        o.setOwnerUserId(1L); o.setDeptId(1L);
        o.setDeliveryDate(LocalDate.now().plusDays(7));
        CrmOrderItem item = new CrmOrderItem();
        item.setQuantity(50000); item.setUnitPrice(new BigDecimal("1000"));
        Result<CrmOrder> r = svc.createOrder(o, List.of(item), 1L);
        assertEquals(0, r.getCode());
    }
}
