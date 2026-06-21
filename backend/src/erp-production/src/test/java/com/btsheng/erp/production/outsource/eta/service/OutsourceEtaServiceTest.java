package com.btsheng.erp.production.outsource.eta.service;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.eta.dto.PredictEtaRequest;
import com.btsheng.erp.production.outsource.eta.entity.CrmOutsourceActual;
import com.btsheng.erp.production.outsource.eta.entity.CrmOutsourceEta;
import com.btsheng.erp.production.outsource.eta.mapper.CrmOutsourceActualMapper;
import com.btsheng.erp.production.outsource.eta.mapper.CrmOutsourceEtaMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.24 · OutsourceEtaService 单元测试（FR-6-4）
 * 10 测例：predict + 准确率 + 偏差告警 + 历史 + 审计 + 跨模块
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutsourceEtaServiceTest {

    @Mock private CrmOutsourceEtaMapper etaMapper;
    @Mock private CrmOutsourceActualMapper actualMapper;
    @Mock private CrmOutsourceOrderMapper orderMapper;
    @Mock private ErpDocNoGenerator docNoGenerator;

    private OutsourceEtaService service;

    @BeforeEach
    void setUp() {
        service = new OutsourceEtaService(etaMapper, actualMapper, orderMapper, docNoGenerator);
        when(docNoGenerator.nextOutsourceEtaNo()).thenReturn("OE20260612-0001", "OE20260612-0002", "OE20260612-0003");

        when(etaMapper.insert(any(CrmOutsourceEta.class))).thenAnswer(inv -> {
            CrmOutsourceEta e = inv.getArgument(0);
            e.setId(1L);
            return 1;
        });
        when(actualMapper.insert(any(CrmOutsourceActual.class))).thenAnswer(inv -> {
            CrmOutsourceActual a = inv.getArgument(0);
            a.setId(1L);
            return 1;
        });
    }

    private CrmOutsourceOrder mockOrder() {
        CrmOutsourceOrder o = new CrmOutsourceOrder();
        o.setId(1L);
        o.setOutsourceNo("WW20260612-0001");
        o.setSupplierId(101L);
        o.setSupplierName("上海热处理厂");
        o.setProcessName("调质");
        o.setQty(10);
        o.setStatus("SENT");
        return o;
    }

    private CrmOutsourceActual mockActual(int days) {
        CrmOutsourceActual a = new CrmOutsourceActual();
        a.setActualDays(days);
        return a;
    }

    // ====== predictEta 5 测例 ======
            @Test
    @DisplayName("predictEta happy path：基于历史样本计算")
    void testPredict_Happy() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder());
        when(actualMapper.selectBySupplierAndProcess(101L, "调质", 50))
                .thenReturn(List.of(mockActual(8), mockActual(10), mockActual(12)));

        PredictEtaRequest req = new PredictEtaRequest();
        req.setOutsourceId(1L);
        req.setStartDate(LocalDate.of(2026, 6, 12));

        Result<CrmOutsourceEta> r = service.predictEta(req, 201L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData());
        assertEquals(10, r.getData().getPredictedDays());  // (8+10+12)/3 = 10
            assertEquals(3, r.getData().getBaseSamples());
    }

    @Test
    @DisplayName("predictEta 无历史样本 → 默认 10 天")
    void testPredict_NoHistory() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder());
        when(actualMapper.selectBySupplierAndProcess(anyLong(), any(), anyInt()))
                .thenReturn(new ArrayList<>());

        PredictEtaRequest req = new PredictEtaRequest();
        req.setOutsourceId(1L);

        Result<CrmOutsourceEta> r = service.predictEta(req, 201L);
        assertEquals(0, r.getCode());
        assertEquals(10, r.getData().getPredictedDays());
        assertEquals(0, r.getData().getBaseSamples());
    }

    @Test
    @DisplayName("predictEta 委外单不存在 → 40404")
    void testPredict_OrderNotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);

        PredictEtaRequest req = new PredictEtaRequest();
        req.setOutsourceId(999L);

        Result<CrmOutsourceEta> r = service.predictEta(req, 201L);
        assertEquals(40404, r.getCode());
    }

    @Test
    @DisplayName("predictEta 委外 ID 必填 → 40001")
    void testPredict_OutsourceIdRequired() {
        Result<CrmOutsourceEta> r = service.predictEta(new PredictEtaRequest(), 201L);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("predictEta 准确率 ≥ 80% 默认置信度")
    void testPredict_AccuracyConfidence() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder());
        when(actualMapper.selectBySupplierAndProcess(anyLong(), any(), anyInt()))
                .thenReturn(List.of(mockActual(8), mockActual(10), mockActual(10), mockActual(9), mockActual(11)));

        PredictEtaRequest req = new PredictEtaRequest();
        req.setOutsourceId(1L);

        Result<CrmOutsourceEta> r = service.predictEta(req, 201L);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().getConfidence().compareTo(new BigDecimal("0.80")) >= 0);
    }

    // ====== updateActualEta 4 测例 ======
            @Test
    @DisplayName("updateActualEta 偏差 0% → COMPLETED")
    void testActual_ZeroDeviation() {
        CrmOutsourceEta eta = new CrmOutsourceEta();
        eta.setId(1L);
        eta.setOutsourceId(1L);
        eta.setOutsourceNo("WW20260612-0001");
        eta.setSupplierId(101L);
        eta.setPredictedDays(10);
        eta.setPredictedDeliveryDate(LocalDate.of(2026, 6, 22));
        eta.setCreatedAt(java.time.LocalDateTime.of(2026, 6, 12, 0, 0));
        when(etaMapper.selectById(1L)).thenReturn(eta);
        when(etaMapper.updateById(any(CrmOutsourceEta.class))).thenReturn(1);

        Result<CrmOutsourceEta> r = service.updateActualEta(1L, LocalDate.of(2026, 6, 22), 201L);
        assertEquals(0, r.getCode());
        assertEquals("COMPLETED", r.getData().getStatus());
    }

    @Test
    @DisplayName("updateActualEta 偏差 > 20% → ALERTED")
    void testActual_OverDeviation() {
        CrmOutsourceEta eta = new CrmOutsourceEta();
        eta.setId(1L);
        eta.setOutsourceId(1L);
        eta.setOutsourceNo("WW20260612-0001");
        eta.setSupplierId(101L);
        eta.setPredictedDays(10);
        eta.setPredictedDeliveryDate(LocalDate.of(2026, 6, 22));
        eta.setCreatedAt(java.time.LocalDateTime.of(2026, 6, 12, 0, 0));
        when(etaMapper.selectById(1L)).thenReturn(eta);
        when(etaMapper.updateById(any(CrmOutsourceEta.class))).thenReturn(1);

        // 实际交付 2026-06-30 → 偏差 (18-10)/10 = 80%
            Result<CrmOutsourceEta> r = service.updateActualEta(1L, LocalDate.of(2026, 6, 30), 201L);
        assertEquals(0, r.getCode());
        assertEquals("ALERTED", r.getData().getStatus());
    }

    @Test
    @DisplayName("updateActualEta ETA 不存在 → 40404")
    void testActual_NotFound() {
        when(etaMapper.selectById(999L)).thenReturn(null);

        Result<CrmOutsourceEta> r = service.updateActualEta(999L, LocalDate.now(), 201L);
        assertEquals(40404, r.getCode());
    }

    @Test
    @DisplayName("updateActualEta 实际日期必填 → 40001")
    void testActual_DateRequired() {
        Result<CrmOutsourceEta> r = service.updateActualEta(1L, null, 201L);
        assertEquals(40001, r.getCode());
    }

    // ====== getEtaHistory 1 测例 ======
            @Test
    @DisplayName("getEtaHistory 返回预估列表 + 准确率")
    void testGetHistory() {
        CrmOutsourceEta e = new CrmOutsourceEta();
        e.setId(1L);
        e.setSupplierId(101L);
        when(etaMapper.selectByOutsourceId(1L)).thenReturn(List.of(e));
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", 1);
        stats.put("passed", 1);
        when(etaMapper.selectAccuracyStats(101L)).thenReturn(stats);

        Result<Map<String, Object>> r = service.getEtaHistory(1L);
        assertEquals(0, r.getCode());
        assertEquals(1, ((List<?>) r.getData().get("etas")).size());
        assertNotNull(r.getData().get("accuracyStats"));
    }
}
