package com.btsheng.erp.production.outsource.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.production.outsource.dto.OutsourceArriveRequest;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceStateHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutsourceReceiveServiceTest {

    private CrmOutsourceOrderMapper orderMapper;
    private CrmOutsourceStateHistoryMapper stateHistoryMapper;
    private OutsourceReceiveService service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(CrmOutsourceOrderMapper.class);
        stateHistoryMapper = mock(CrmOutsourceStateHistoryMapper.class);
        service = new OutsourceReceiveService(orderMapper, stateHistoryMapper);
    }

    @Test
    @DisplayName("normalizeOutsourceNo WW- 前缀")
    void normalizeOutsourceNo() {
        assertEquals("WW20260612-0001", OutsourceReceiveService.normalizeOutsourceNo("WW-20260612-0001"));
        assertEquals("WW20260612-0001", OutsourceReceiveService.normalizeOutsourceNo("ww20260612-0001"));
    }

    @Test
    @DisplayName("SENT 委外单到货 → INSPECTED")
    void receive_fromSent() {
        CrmOutsourceOrder order = new CrmOutsourceOrder();
        order.setId(1L);
        order.setOutsourceNo("WW20260612-0001");
        order.setStatus(OutsourceStateMachineService.STATE_SENT);
        when(orderMapper.selectById(1L)).thenReturn(order);

        OutsourceArriveRequest req = new OutsourceArriveRequest();
        req.setOutsourceNo("WW20260612-0001");
        req.setActualQty(10);
        req.setActualWeight(new BigDecimal("5.5"));

        Result<CrmOutsourceOrder> result = service.receive(1L, req, 7L);

        assertEquals(0, result.getCode());
        assertEquals(OutsourceStateMachineService.STATE_INSPECTED, result.getData().getStatus());
        verify(orderMapper).updateById(any(CrmOutsourceOrder.class));
        verify(stateHistoryMapper).insert(any(com.btsheng.erp.production.outsource.entity.CrmOutsourceStateHistory.class));
    }
}
