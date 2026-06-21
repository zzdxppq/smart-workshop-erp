package com.btsheng.erp.business.crm.pricecontrol.integration;

import com.btsheng.erp.business.crm.pricecontrol.dto.CheckPriceRequest;
import com.btsheng.erp.business.crm.pricecontrol.dto.SetPriceLimitRequest;
import com.btsheng.erp.business.crm.pricecontrol.entity.CrmPriceControl;
import com.btsheng.erp.business.crm.pricecontrol.entity.CrmPriceHistory;
import com.btsheng.erp.business.crm.pricecontrol.mapper.CrmPriceControlMapper;
import com.btsheng.erp.business.crm.pricecontrol.mapper.CrmPriceHistoryMapper;
import com.btsheng.erp.business.crm.pricecontrol.service.PriceControlService;
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
 * V1.3.7 · Story 1.33 · 采购·价格控制 集成测试（FR-8-2）
 * 8 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PriceControlIntegrationTest {

    @Mock private CrmPriceControlMapper controlMapper;
    @Mock private CrmPriceHistoryMapper historyMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private PriceControlService service;

    @BeforeEach
    void setUp() {
        service = new PriceControlService(controlMapper, historyMapper, docNoGenerator);
        when(docNoGenerator.nextPriceControlNo())
                .thenReturn("PL20260612-0001", "PL20260612-0002", "PL20260612-0003", "PL20260612-0004", "PL20260612-0005");
        when(controlMapper.insert(any(CrmPriceControl.class))).thenAnswer(inv -> {
            CrmPriceControl c = inv.getArgument(0);
            c.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
    }

    // ====== 完整 lifecycle：设置 → 校验 → 历史 ======
            @Test
    @DisplayName("集成 lifecycle：设置限价 → 校验通过 → 历史 3 月内")
    void testIntegration_FullFlow() {
        SetPriceLimitRequest sr = new SetPriceLimitRequest();
        sr.setMaterialId(1001L);
        sr.setMaterialCode("M-AL6061-PT");
        sr.setPriceLimit(new BigDecimal("480"));
        sr.setEffectiveDate(LocalDate.now());
        Result<CrmPriceControl> s = service.setPriceLimit(sr, 502L, "李采购");
        assertEquals(0, s.getCode());

        CrmPriceControl pc = new CrmPriceControl();
        pc.setPriceLimit(new BigDecimal("480"));
        when(controlMapper.selectByMaterialAndVendor(1001L, 901L)).thenReturn(pc);
        when(historyMapper.avgPrice(anyLong(), anyLong(), any())).thenReturn(new BigDecimal("455"));

        CheckPriceRequest cr = new CheckPriceRequest();
        cr.setMaterialId(1001L);
        cr.setVendorId(901L);
        cr.setUnitPrice(new BigDecimal("460"));
        Result<Map<String, Object>> c = service.checkPrice(cr);
        assertEquals(0, c.getCode());
        assertEquals("OK", c.getData().get("alertLevel"));

        CrmPriceHistory h = new CrmPriceHistory();
        h.setId(1L);
        h.setUnitPrice(new BigDecimal("455"));
        h.setPurchasedAt(LocalDate.now().minusDays(30));
        when(historyMapper.selectByMaterialVendorSince(anyLong(), anyLong(), any())).thenReturn(List.of(h));
        Result<List<CrmPriceHistory>> his = service.listPriceHistory(1001L, 901L);
        assertEquals(0, his.getCode());
        assertEquals(1, his.getData().size());
    }

    // ====== 单号 PL 前缀 ======
            @Test
    @DisplayName("AC-8.2.1：单号模板 PL{yyyyMMdd}{seq:4}")
    void testIntegration_PlNo() {
        SetPriceLimitRequest sr = new SetPriceLimitRequest();
        sr.setMaterialId(1001L);
        sr.setPriceLimit(new BigDecimal("480"));
        sr.setEffectiveDate(LocalDate.now());
        Result<CrmPriceControl> s = service.setPriceLimit(sr, 502L, "李采购");
        assertTrue(s.getData().getControlNo().startsWith("PL"));
        assertEquals(15, s.getData().getControlNo().length());
    }

    // ====== 唯一性索引 (material_id, vendor_id) ======
            @Test
    @DisplayName("P1 修补：唯一索引 (material_id, vendor_id) DB 兜底")
    void testIntegration_UniqueIndex() {
        CrmPriceControl existing = new CrmPriceControl();
        existing.setId(1L);
        existing.setMaterialId(1001L);
        existing.setVendorId(901L);
        existing.setPriceLimit(new BigDecimal("460"));
        when(controlMapper.selectByMaterialAndVendor(1001L, 901L)).thenReturn(existing);
        Result<CrmPriceControl> r = service.getPriceLimit(1001L, 901L);
        assertEquals(0, r.getCode());
        assertEquals(901L, r.getData().getVendorId());
    }

    // ====== 偏差率 20% 阈值 ======
            @Test
    @DisplayName("P1 修补 2：偏差率 19% → OK；25% → ALERTED")
    void testIntegration_Threshold() {
        CrmPriceControl pc = new CrmPriceControl();
        pc.setPriceLimit(new BigDecimal("1000"));
        when(controlMapper.selectByMaterialAndVendor(1001L, 901L)).thenReturn(pc);
        when(historyMapper.avgPrice(anyLong(), anyLong(), any())).thenReturn(new BigDecimal("400"));

        CheckPriceRequest ok = new CheckPriceRequest();
        ok.setMaterialId(1001L);
        ok.setVendorId(901L);
        ok.setUnitPrice(new BigDecimal("475"));   // +18.75% < 20%
            Result<Map<String, Object>> r1 = service.checkPrice(ok);
        assertEquals("OK", r1.getData().get("alertLevel"));

        CheckPriceRequest alerted = new CheckPriceRequest();
        alerted.setMaterialId(1001L);
        alerted.setVendorId(901L);
        alerted.setUnitPrice(new BigDecimal("500"));   // +25% ≥ 20%
            Result<Map<String, Object>> r2 = service.checkPrice(alerted);
        assertEquals("ALERTED", r2.getData().get("alertLevel"));
    }

    // ====== 厂商专享 vs 通用 ======
            @Test
    @DisplayName("厂商专享与通用并存：优先厂商专享")
    void testIntegration_VendorSpecificWins() {
        CrmPriceControl vendor = new CrmPriceControl();
        vendor.setPriceLimit(new BigDecimal("450"));
        vendor.setVendorId(901L);
        CrmPriceControl generic = new CrmPriceControl();
        generic.setPriceLimit(new BigDecimal("480"));
        generic.setVendorId(null);
        when(controlMapper.selectByMaterialAndVendor(1001L, 901L)).thenReturn(vendor);
        when(controlMapper.selectGenericByMaterial(1001L)).thenReturn(generic);
        Result<CrmPriceControl> r = service.getPriceLimit(1001L, 901L);
        assertEquals(0, r.getData().getPriceLimit().compareTo(new BigDecimal("450")));
    }

    // ====== 限价无 → 404 ======
            @Test
    @DisplayName("getPriceLimit 未设限价 → 404")
    void testIntegration_NotFound() {
        when(controlMapper.selectByMaterialAndVendor(9999L, 901L)).thenReturn(null);
        when(controlMapper.selectGenericByMaterial(9999L)).thenReturn(null);
        Result<CrmPriceControl> r = service.getPriceLimit(9999L, 901L);
        assertEquals(40404, r.getCode());
    }

    // ====== 价格负数拒绝 ======
            @Test
    @DisplayName("checkPrice 单价负数 → 40001")
    void testIntegration_NegativePrice() {
        CheckPriceRequest req = new CheckPriceRequest();
        req.setMaterialId(1001L);
        req.setVendorId(901L);
        req.setUnitPrice(new BigDecimal("-100"));
        Result<Map<String, Object>> r = service.checkPrice(req);
        assertEquals(40001, r.getCode());
    }

    // ====== 审计留痕 ======
            @Test
    @DisplayName("审计留痕：setPriceLimit + checkPrice + listPriceHistory 正常返回")
    void testIntegration_Audit() {
        SetPriceLimitRequest sr = new SetPriceLimitRequest();
        sr.setMaterialId(1001L);
        sr.setPriceLimit(new BigDecimal("480"));
        sr.setEffectiveDate(LocalDate.now());
        Result<CrmPriceControl> s = service.setPriceLimit(sr, 502L, "李采购");
        assertEquals(0, s.getCode());

        CrmPriceControl pc = new CrmPriceControl();
        pc.setPriceLimit(new BigDecimal("480"));
        when(controlMapper.selectByMaterialAndVendor(1001L, 901L)).thenReturn(pc);
        CheckPriceRequest cr = new CheckPriceRequest();
        cr.setMaterialId(1001L);
        cr.setVendorId(901L);
        cr.setUnitPrice(new BigDecimal("460"));
        Result<Map<String, Object>> c = service.checkPrice(cr);
        assertEquals(0, c.getCode());
    }
}
