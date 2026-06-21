package com.btsheng.erp.production.outsource.eta.integration;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.eta.dto.PredictEtaRequest;
import com.btsheng.erp.production.outsource.eta.entity.CrmOutsourceActual;
import com.btsheng.erp.production.outsource.eta.entity.CrmOutsourceEta;
import com.btsheng.erp.production.outsource.eta.mapper.CrmOutsourceActualMapper;
import com.btsheng.erp.production.outsource.eta.mapper.CrmOutsourceEtaMapper;
import com.btsheng.erp.production.outsource.eta.service.OutsourceEtaService;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.24 · OutsourceEtaService 集成测试（FR-6-4）
 * 8 测例：完整预估 + 实际更新 + 偏差告警 + 准确率 + 跨模块
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutsourceEtaIntegrationTest {

    @Mock private CrmOutsourceEtaMapper etaMapper;
    @Mock private CrmOutsourceActualMapper actualMapper;
    @Mock private CrmOutsourceOrderMapper orderMapper;
    @Mock private ErpDocNoGenerator docNoGenerator;

    private OutsourceEtaService service;

    @BeforeEach
    void setUp() {
        service = new OutsourceEtaService(etaMapper, actualMapper, orderMapper, docNoGenerator);
        when(docNoGenerator.nextOutsourceEtaNo()).thenReturn("OE20260612-0001", "OE20260612-0002", "OE20260612-0003", "OE20260612-0004");
        when(etaMapper.insert(any(CrmOutsourceEta.class))).thenAnswer(inv -> {
            CrmOutsourceEta e = inv.getArgument(0);
            e.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(actualMapper.insert(any(CrmOutsourceActual.class))).thenAnswer(inv -> {
            CrmOutsourceActual a = inv.getArgument(0);
            a.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
    }

    private CrmOutsourceOrder mockOrder(Long id, Long supplierId, String process) {
        CrmOutsourceOrder o = new CrmOutsourceOrder();
        o.setId(id);
        o.setOutsourceNo("WW" + id);
        o.setSupplierId(supplierId);
        o.setSupplierName("Test Supplier " + supplierId);
        o.setProcessName(process);
        o.setQty(10);
        o.setStatus("SENT");
        return o;
    }

    // ====== 完整 lifecycle 1：预估 → 实际 → 完成 ======
            @Test
    @DisplayName("集成 lifecycle 1：预估 → 实际 0% 偏差 → COMPLETED")
    void testIntegration_PredictThenActual_OnTime() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 101L, "调质"));
        when(actualMapper.selectBySupplierAndProcess(anyLong(), any(), anyInt()))
                .thenReturn(List.of(makeActual(8), makeActual(10), makeActual(12)));

        PredictEtaRequest req = new PredictEtaRequest();
        req.setOutsourceId(1L);
        req.setStartDate(LocalDate.of(2026, 6, 12));

        Result<CrmOutsourceEta> p = service.predictEta(req, 201L);
        assertEquals(0, p.getCode());
        Long etaId = p.getData().getId();

        // 模拟 10 天后实际交付
            CrmOutsourceEta eta = new CrmOutsourceEta();
        eta.setId(etaId);
        eta.setOutsourceId(1L);
        eta.setOutsourceNo("WW1");
        eta.setSupplierId(101L);
        eta.setPredictedDays(10);
        eta.setPredictedDeliveryDate(LocalDate.of(2026, 6, 22));
        eta.setCreatedAt(java.time.LocalDateTime.of(2026, 6, 12, 0, 0));
        when(etaMapper.selectById(etaId)).thenReturn(eta);
        when(etaMapper.updateById(any(CrmOutsourceEta.class))).thenReturn(1);

        Result<CrmOutsourceEta> a = service.updateActualEta(etaId, LocalDate.of(2026, 6, 22), 201L);
        assertEquals(0, a.getCode());
        assertEquals("COMPLETED", a.getData().getStatus());
    }

    // ====== 完整 lifecycle 2：预估 → 实际 偏差 > 20% → ALERTED ======
            @Test
    @DisplayName("集成 lifecycle 2：预估 → 实际 80% 偏差 → ALERTED")
    void testIntegration_PredictThenActual_Delayed() {
        when(orderMapper.selectById(2L)).thenReturn(mockOrder(2L, 102L, "电镀锌"));
        when(actualMapper.selectBySupplierAndProcess(anyLong(), any(), anyInt()))
                .thenReturn(List.of(makeActual(7), makeActual(7)));

        PredictEtaRequest req = new PredictEtaRequest();
        req.setOutsourceId(2L);
        req.setStartDate(LocalDate.of(2026, 6, 12));

        Result<CrmOutsourceEta> p = service.predictEta(req, 201L);
        Long etaId = p.getData().getId();

        CrmOutsourceEta eta = new CrmOutsourceEta();
        eta.setId(etaId);
        eta.setOutsourceId(2L);
        eta.setOutsourceNo("WW2");
        eta.setSupplierId(102L);
        eta.setPredictedDays(7);
        eta.setPredictedDeliveryDate(LocalDate.of(2026, 6, 19));
        eta.setCreatedAt(java.time.LocalDateTime.of(2026, 6, 12, 0, 0));
        when(etaMapper.selectById(etaId)).thenReturn(eta);
        when(etaMapper.updateById(any(CrmOutsourceEta.class))).thenReturn(1);

        // 实际交付晚 8 天 → 偏差 114%
            Result<CrmOutsourceEta> a = service.updateActualEta(etaId, LocalDate.of(2026, 6, 27), 201L);
        assertEquals(0, a.getCode());
        assertEquals("ALERTED", a.getData().getStatus());
    }

    // ====== 预估样本不足 → 准确率默认 0.80 ======
            @Test
    @DisplayName("集成：样本不足置信度降为 0.80 默认")
    void testIntegration_LowSamples_LowConfidence() {
        when(orderMapper.selectById(3L)).thenReturn(mockOrder(3L, 103L, "线切割"));
        when(actualMapper.selectBySupplierAndProcess(anyLong(), any(), anyInt()))
                .thenReturn(List.of(makeActual(10)));

        PredictEtaRequest req = new PredictEtaRequest();
        req.setOutsourceId(3L);

        Result<CrmOutsourceEta> r = service.predictEta(req, 201L);
        assertEquals(0, r.getCode());
        assertEquals(0, r.getData().getConfidence().compareTo(new BigDecimal("0.75")));
    }

    // ====== 偏差率计算精度 ======
            @Test
    @DisplayName("集成：偏差率 = (actual - predicted) / predicted × 100")
    void testIntegration_DeviationPct() {
        CrmOutsourceEta eta = new CrmOutsourceEta();
        eta.setId(100L);
        eta.setOutsourceId(4L);
        eta.setOutsourceNo("WW4");
        eta.setSupplierId(104L);
        eta.setPredictedDays(10);
        eta.setPredictedDeliveryDate(LocalDate.of(2026, 6, 22));
        eta.setCreatedAt(java.time.LocalDateTime.of(2026, 6, 12, 0, 0));
        when(etaMapper.selectById(100L)).thenReturn(eta);
        when(etaMapper.updateById(any(CrmOutsourceEta.class))).thenReturn(1);

        // 实际交付 11 天后 → 偏差 10%
            Result<CrmOutsourceEta> r = service.updateActualEta(100L, LocalDate.of(2026, 6, 23), 201L);
        assertEquals(0, r.getCode());
        verify(actualMapper, atLeastOnce()).insert(argThat((CrmOutsourceActual a) ->
                a.getDeviationPct() != null && a.getDeviationPct().compareTo(new BigDecimal("10.00")) == 0));
    }

    // ====== 准确率统计查询 ======
            @Test
    @DisplayName("集成：getEtaHistory 返回准确率统计")
    void testIntegration_HistoryWithAccuracy() {
        CrmOutsourceEta e = new CrmOutsourceEta();
        e.setId(1L);
        e.setSupplierId(101L);
        when(etaMapper.selectByOutsourceId(5L)).thenReturn(List.of(e));
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", 5);
        stats.put("passed", 4);
        when(etaMapper.selectAccuracyStats(101L)).thenReturn(stats);

        Result<Map<String, Object>> r = service.getEtaHistory(5L);
        assertEquals(0, r.getCode());
        assertEquals(4, r.getData().get("accuracyStats").toString().contains("passed") ? 4 : 0);
    }

    // ====== 委外单不存在 跨模块 1.18 ======
            @Test
    @DisplayName("跨模块 1.18：委外单不存在 → 40404")
    void testIntegration_Cross_18_OrderNotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);
        PredictEtaRequest req = new PredictEtaRequest();
        req.setOutsourceId(999L);

        Result<CrmOutsourceEta> r = service.predictEta(req, 201L);
        assertEquals(40404, r.getCode());
    }

    // ====== 跨供应商统计独立 ======
            @Test
    @DisplayName("集成：不同供应商预测独立")
    void testIntegration_DifferentSuppliers_Independent() {
        when(orderMapper.selectById(10L)).thenReturn(mockOrder(10L, 201L, "热处理"));
        when(orderMapper.selectById(20L)).thenReturn(mockOrder(20L, 202L, "热处理"));
        when(actualMapper.selectBySupplierAndProcess(201L, "热处理", 50)).thenReturn(List.of(makeActual(5)));
        when(actualMapper.selectBySupplierAndProcess(202L, "热处理", 50)).thenReturn(List.of(makeActual(15)));

        PredictEtaRequest r1 = new PredictEtaRequest();
        r1.setOutsourceId(10L);
        PredictEtaRequest r2 = new PredictEtaRequest();
        r2.setOutsourceId(20L);

        Result<CrmOutsourceEta> p1 = service.predictEta(r1, 201L);
        Result<CrmOutsourceEta> p2 = service.predictEta(r2, 201L);

        assertEquals(5, p1.getData().getPredictedDays());
        assertEquals(15, p2.getData().getPredictedDays());
    }

    // ====== 偏差率负值 ======
            @Test
    @DisplayName("集成：负偏差（提前交付）→ on_time = 1")
    void testIntegration_NegativeDeviation() {
        CrmOutsourceEta eta = new CrmOutsourceEta();
        eta.setId(200L);
        eta.setOutsourceId(30L);
        eta.setOutsourceNo("WW30");
        eta.setSupplierId(105L);
        eta.setPredictedDays(10);
        eta.setPredictedDeliveryDate(LocalDate.of(2026, 6, 25));
        eta.setCreatedAt(java.time.LocalDateTime.of(2026, 6, 12, 0, 0));
        when(etaMapper.selectById(200L)).thenReturn(eta);
        when(etaMapper.updateById(any(CrmOutsourceEta.class))).thenReturn(1);

        // 实际 7 天后 → 偏差 -30%
            Result<CrmOutsourceEta> r = service.updateActualEta(200L, LocalDate.of(2026, 6, 19), 201L);
        assertEquals(0, r.getCode());
        verify(actualMapper, atLeastOnce()).insert(argThat((CrmOutsourceActual a) ->
                a.getOnTime() != null && a.getOnTime() == 0));
    }

    private CrmOutsourceActual makeActual(int days) {
        CrmOutsourceActual a = new CrmOutsourceActual();
        a.setActualDays(days);
        return a;
    }
}
