package com.btsheng.erp.business.crm.outsourcecost.integration;

import com.btsheng.erp.business.integration.client.OutsourceOrderClient;
import com.btsheng.erp.core.integration.dto.OutsourceOrderRef;
import com.btsheng.erp.business.crm.outsourcecost.dto.AggregateCostRequest;
import com.btsheng.erp.business.crm.outsourcecost.entity.CrmOutsourceCostAggregation;
import com.btsheng.erp.business.crm.outsourcecost.mapper.CrmOutsourceCostAggregationMapper;
import com.btsheng.erp.business.crm.outsourcecost.service.OutsourceCostAggregationService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.26 · 委外成本归集 集成测试（FR-6-6）
 * 10 测例：完整归集 + 5 段聚合 + 跨模块 1.10 + 偏差率
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutsourceCostAggregationIntegrationTest {

    @Mock private CrmOutsourceCostAggregationMapper mapper;
    @Mock private OutsourceOrderClient outsourceOrderClient;

    private OutsourceCostAggregationService service;

    @BeforeEach
    void setUp() {
        service = new OutsourceCostAggregationService(mapper, outsourceOrderClient);
        when(mapper.insert(any(CrmOutsourceCostAggregation.class))).thenAnswer(inv -> {
            CrmOutsourceCostAggregation a = inv.getArgument(0);
            a.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
    }

    private OutsourceOrderRef mockOrder(Long id) {
        OutsourceOrderRef o = new OutsourceOrderRef();
        o.setId(id);
        o.setOutsourceNo("WW" + id);
        o.setStatus("COMPLETED");
        return o;
    }

    private void stubOrder(Long id) {
        when(outsourceOrderClient.getById(id)).thenReturn(Result.ok(mockOrder(id)));
    }

    private AggregateCostRequest buildReq(Long outsourceId, BigDecimal material, BigDecimal labor,
                                            BigDecimal machine, BigDecimal overhead, BigDecimal outsource,
                                            BigDecimal budget) {
        AggregateCostRequest req = new AggregateCostRequest();
        req.setOutsourceId(outsourceId);
        req.setMaterialCode("ZZ-0001");
        req.setProcessName("调质");
        req.setCostMaterial(material);
        req.setCostLabor(labor);
        req.setCostMachine(machine);
        req.setCostOverhead(overhead);
        req.setCostOutsource(outsource);
        req.setBudgetCost(budget);
        req.setAggregationScope("PROCESS");
        return req;
    }

    // ====== 完整 lifecycle 1：归集 → 段聚合 ======
            @Test
    @DisplayName("集成 lifecycle 1：归集 → 5 段聚合 → 报表")
    void testIntegration_Lifecycle_AggregateThenSegment() {
        stubOrder(1L);

        AggregateCostRequest req = buildReq(1L,
                new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("20"),
                new BigDecimal("10"), new BigDecimal("150"), new BigDecimal("300"));
        Result<CrmOutsourceCostAggregation> a = service.aggregateCost(req, 201L);
        assertEquals(0, a.getCode());

        // 5 段聚合查询
            Map<String, Object> seg = new HashMap<>();
        seg.put("sumMaterial", 100);
        seg.put("sumLabor", 30);
        seg.put("sumMachine", 20);
        seg.put("sumOverhead", 10);
        seg.put("sumOutsource", 150);
        seg.put("sumTotal", 310);
        seg.put("sumBudget", 300);
        when(mapper.selectSegmentSumByOutsourceId(1L)).thenReturn(seg);

        Result<Map<String, Object>> s = service.getCostBySegment(1L);
        assertEquals(0, s.getCode());
        assertEquals(310, s.getData().get("sumTotal"));
    }

    // ====== 5 段自动累加精度 ======
            @Test
    @DisplayName("集成：5 段自动累加精度 = 310（100+30+20+10+150）")
    void testIntegration_SumPrecision() {
        stubOrder(1L);

        AggregateCostRequest req = buildReq(1L,
                new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("20"),
                new BigDecimal("10"), new BigDecimal("150"), new BigDecimal("310"));
        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals(0, r.getCode());
        assertEquals(0, r.getData().getCostTotal().compareTo(new BigDecimal("310")));
    }

    // ====== 偏差率计算 ======
            @Test
    @DisplayName("集成：偏差率 = (actual - budget) / budget × 100")
    void testIntegration_DeviationPct() {
        stubOrder(2L);

        AggregateCostRequest req = buildReq(2L,
                new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("20"),
                new BigDecimal("10"), new BigDecimal("150"), new BigDecimal("250"));
        // actual=310, budget=250 → 24%
            Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals("OVER", r.getData().getDeviationLevel());
    }

    @Test
    @DisplayName("集成：负偏差（节省成本）")
    void testIntegration_NegativeDeviation() {
        stubOrder(3L);

        AggregateCostRequest req = buildReq(3L,
                new BigDecimal("50"), new BigDecimal("20"), new BigDecimal("10"),
                new BigDecimal("5"), new BigDecimal("75"), new BigDecimal("200"));
        // actual=160, budget=200 → -20%
            Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals("OVER", r.getData().getDeviationLevel());  // abs 20% > 10% → OVER
    }

    // ====== 跨模块 1.10 5 段聚合闭环 ======
            @Test
    @DisplayName("跨模块 1.10：5 段聚合返回 sumMaterial / sumLabor / sumMachine / sumOverhead / sumOutsource")
    void testIntegration_Cross_10_5Segment() {
        Map<String, Object> seg = new HashMap<>();
        seg.put("sumMaterial", 200);
        seg.put("sumLabor", 60);
        seg.put("sumMachine", 40);
        seg.put("sumOverhead", 20);
        seg.put("sumOutsource", 300);
        seg.put("sumTotal", 620);
        seg.put("sumBudget", 600);
        when(mapper.selectSegmentSumByOutsourceId(1L)).thenReturn(seg);

        Result<Map<String, Object>> r = service.getCostBySegment(1L);
        assertEquals(0, r.getCode());
        assertEquals(200, r.getData().get("sumMaterial"));
        assertEquals(60, r.getData().get("sumLabor"));
        assertEquals(40, r.getData().get("sumMachine"));
        assertEquals(20, r.getData().get("sumOverhead"));
        assertEquals(300, r.getData().get("sumOutsource"));
    }

    // ====== 跨模块 1.18 委外单联动 ======
            @Test
    @DisplayName("跨模块 1.18：委外单不存在 → 40404")
    void testIntegration_Cross_18_OrderNotFound() {
        when(outsourceOrderClient.getById(999L)).thenReturn(Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND"));
        AggregateCostRequest req = buildReq(999L,
                new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("20"),
                new BigDecimal("10"), new BigDecimal("150"), new BigDecimal("300"));

        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals(40404, r.getCode());
    }

    // ====== 审计留痕 ======
            @Test
    @DisplayName("审计：aggregateCost 写 1 行归集")
    void testIntegration_Audit() {
        stubOrder(4L);

        AggregateCostRequest req = buildReq(4L,
                new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("20"),
                new BigDecimal("10"), new BigDecimal("150"), new BigDecimal("300"));

        service.aggregateCost(req, 201L);
        verify(mapper, times(1)).insert(any(CrmOutsourceCostAggregation.class));
    }

    // ====== STEP / PROCESS / WHOLE 3 范围 ======
            @Test
    @DisplayName("集成：STEP / PROCESS / WHOLE 3 种 scope")
    void testIntegration_ThreeScopes() {
        stubOrder(5L);
        for (String scope : new String[]{"STEP", "PROCESS", "WHOLE"}) {
            AggregateCostRequest req = buildReq(5L,
                    new BigDecimal("10"), new BigDecimal("3"), new BigDecimal("2"),
                    new BigDecimal("1"), new BigDecimal("15"), new BigDecimal("30"));
            req.setAggregationScope(scope);
            Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
            assertEquals(0, r.getCode());
            assertEquals(scope, r.getData().getAggregationScope());
        }
    }

    // ====== 5 段任一为 0 也允许 ======
            @Test
    @DisplayName("集成：5 段允许为 0（非负）")
    void testIntegration_ZeroAllowed() {
        stubOrder(6L);
        AggregateCostRequest req = buildReq(6L,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("100"));
        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals(0, r.getCode());
    }

    // ====== 偏差率 WITHIN 边界：恰好 5% ======
            @Test
    @DisplayName("集成：偏差率 5% 边界 → WARN")
    void testIntegration_DeviationWarnEdge() {
        stubOrder(7L);
        AggregateCostRequest req = buildReq(7L,
                new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("20"),
                new BigDecimal("10"), new BigDecimal("150"), new BigDecimal("295"));
        // actual=310, budget=295 → 5.08% → WARN
            Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals("WARN", r.getData().getDeviationLevel());
    }
}
