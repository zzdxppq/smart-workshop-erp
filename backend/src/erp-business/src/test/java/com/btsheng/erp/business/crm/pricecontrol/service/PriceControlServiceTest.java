package com.btsheng.erp.business.crm.pricecontrol.service;

import com.btsheng.erp.business.crm.pricecontrol.dto.CheckPriceRequest;
import com.btsheng.erp.business.crm.pricecontrol.dto.SetPriceLimitRequest;
import com.btsheng.erp.business.crm.pricecontrol.entity.CrmPriceControl;
import com.btsheng.erp.business.crm.pricecontrol.entity.CrmPriceHistory;
import com.btsheng.erp.business.crm.pricecontrol.mapper.CrmPriceControlMapper;
import com.btsheng.erp.business.crm.pricecontrol.mapper.CrmPriceHistoryMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * V1.3.7 · Story 1.33 · 采购·价格控制 Service 单元测试（FR-8-2）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PriceControlServiceTest {

    @Mock private CrmPriceControlMapper controlMapper;
    @Mock private CrmPriceHistoryMapper historyMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private PriceControlService service;

    @BeforeEach
    void setUp() {
        service = new PriceControlService(controlMapper, historyMapper, docNoGenerator);
        when(docNoGenerator.nextPriceControlNo())
                .thenReturn("PL20260612-0001", "PL20260612-0002", "PL20260612-0003");
        when(controlMapper.insert(any(CrmPriceControl.class))).thenAnswer(inv -> {
            CrmPriceControl c = inv.getArgument(0);
            c.setId(1L);
            return 1;
        });
    }

    private SetPriceLimitRequest buildValid() {
        SetPriceLimitRequest r = new SetPriceLimitRequest();
        r.setMaterialId(1001L);
        r.setMaterialCode("M-AL6061-PT");
        r.setPriceLimit(new BigDecimal("480"));
        r.setEffectiveDate(LocalDate.now());
        return r;
    }

    // ====== setPriceLimit 3 测例 ======
            @Test
    @DisplayName("setPriceLimit happy path · 单号 PL 前缀")
    void testSet_OK() {
        Result<CrmPriceControl> r = service.setPriceLimit(buildValid(), 502L, "李采购");
        assertEquals(0, r.getCode());
        assertEquals("PL20260612-0001", r.getData().getControlNo());
    }

    @Test
    @DisplayName("P1 修补 1：限价负数 → 40001")
    void testSet_Negative() {
        SetPriceLimitRequest r = buildValid();
        r.setPriceLimit(new BigDecimal("-1"));
        Result<CrmPriceControl> result = service.setPriceLimit(r, 502L, "李采购");
        assertEquals(40001, result.getCode());
        assertEquals("PRICE_LIMIT_NEGATIVE", result.getMessage());
    }

    @Test
    @DisplayName("setPriceLimit 缺生效日 → 40001")
    void testSet_NoEffectiveDate() {
        SetPriceLimitRequest r = buildValid();
        r.setEffectiveDate(null);
        Result<CrmPriceControl> result = service.setPriceLimit(r, 502L, "李采购");
        assertEquals(40001, result.getCode());
    }

    // ====== getPriceLimit 2 测例 ======
            @Test
    @DisplayName("getPriceLimit 优先取厂商专享")
    void testGet_VendorSpecific() {
        CrmPriceControl pc = new CrmPriceControl();
        pc.setId(1L);
        pc.setPriceLimit(new BigDecimal("460"));
        pc.setVendorId(901L);
        when(controlMapper.selectByMaterialAndVendor(1001L, 901L)).thenReturn(pc);
        Result<CrmPriceControl> r = service.getPriceLimit(1001L, 901L);
        assertEquals(0, r.getCode());
        assertEquals(0, r.getData().getPriceLimit().compareTo(new BigDecimal("460")));
    }

    @Test
    @DisplayName("getPriceLimit 无厂商专享 → 取通用")
    void testGet_Generic() {
        CrmPriceControl pc = new CrmPriceControl();
        pc.setId(2L);
        pc.setPriceLimit(new BigDecimal("480"));
        when(controlMapper.selectByMaterialAndVendor(1001L, 901L)).thenReturn(null);
        when(controlMapper.selectGenericByMaterial(1001L)).thenReturn(pc);
        Result<CrmPriceControl> r = service.getPriceLimit(1001L, 901L);
        assertEquals(0, r.getCode());
        assertEquals(0, r.getData().getPriceLimit().compareTo(new BigDecimal("480")));
    }

    // ====== checkPrice 4 测例 ======
            @Test
    @DisplayName("checkPrice 正常 · OK")
    void testCheck_OK() {
        CrmPriceControl pc = new CrmPriceControl();
        pc.setPriceLimit(new BigDecimal("480"));
        when(controlMapper.selectByMaterialAndVendor(1001L, 901L)).thenReturn(pc);
        when(historyMapper.avgPrice(anyLong(), anyLong(), any())).thenReturn(new BigDecimal("455"));

        CheckPriceRequest req = new CheckPriceRequest();
        req.setMaterialId(1001L);
        req.setVendorId(901L);
        req.setUnitPrice(new BigDecimal("460"));

        Result<Map<String, Object>> r = service.checkPrice(req);
        assertEquals(0, r.getCode());
        assertEquals("OK", r.getData().get("alertLevel"));
    }

    @Test
    @DisplayName("P1 修补 2：偏差率 ≥ 20% ALERTED")
    void testCheck_DeviationAlert() {
        CrmPriceControl pc = new CrmPriceControl();
        pc.setPriceLimit(new BigDecimal("600"));
        when(controlMapper.selectByMaterialAndVendor(1001L, 901L)).thenReturn(pc);
        // 历史均价 400，新单价 500 → 偏差 25% ≥ 20%
            when(historyMapper.avgPrice(anyLong(), anyLong(), any())).thenReturn(new BigDecimal("400"));

        CheckPriceRequest req = new CheckPriceRequest();
        req.setMaterialId(1001L);
        req.setVendorId(901L);
        req.setUnitPrice(new BigDecimal("500"));

        Result<Map<String, Object>> r = service.checkPrice(req);
        assertEquals(0, r.getCode());
        assertEquals("ALERTED", r.getData().get("alertLevel"));
    }

    @Test
    @DisplayName("超限价 → OVER_LIMIT")
    void testCheck_OverLimit() {
        CrmPriceControl pc = new CrmPriceControl();
        pc.setPriceLimit(new BigDecimal("400"));
        when(controlMapper.selectByMaterialAndVendor(1001L, 901L)).thenReturn(pc);
        when(historyMapper.avgPrice(anyLong(), anyLong(), any())).thenReturn(new BigDecimal("390"));

        CheckPriceRequest req = new CheckPriceRequest();
        req.setMaterialId(1001L);
        req.setVendorId(901L);
        req.setUnitPrice(new BigDecimal("500"));

        Result<Map<String, Object>> r = service.checkPrice(req);
        assertEquals(0, r.getCode());
        assertEquals("OVER_LIMIT", r.getData().get("alertLevel"));
        assertEquals(true, r.getData().get("overLimit"));
    }

    @Test
    @DisplayName("无历史价 + 不超限价 → OK")
    void testCheck_NoHistory() {
        CrmPriceControl pc = new CrmPriceControl();
        pc.setPriceLimit(new BigDecimal("600"));
        when(controlMapper.selectByMaterialAndVendor(1001L, 901L)).thenReturn(pc);
        when(historyMapper.avgPrice(anyLong(), anyLong(), any())).thenReturn(null);

        CheckPriceRequest req = new CheckPriceRequest();
        req.setMaterialId(1001L);
        req.setVendorId(901L);
        req.setUnitPrice(new BigDecimal("500"));

        Result<Map<String, Object>> r = service.checkPrice(req);
        assertEquals(0, r.getCode());
        assertEquals("OK", r.getData().get("alertLevel"));
    }

    // ====== listPriceHistory 1 测例 ======
            @Test
    @DisplayName("P1 修补 3：历史价 3 月内")
    void testList_3Months() {
        CrmPriceHistory h = new CrmPriceHistory();
        h.setId(1L);
        h.setUnitPrice(new BigDecimal("455"));
        h.setPurchasedAt(LocalDate.now().minusDays(30));
        when(historyMapper.selectByMaterialVendorSince(anyLong(), anyLong(), any())).thenReturn(List.of(h));
        Result<List<CrmPriceHistory>> r = service.listPriceHistory(1001L, 901L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }
}
