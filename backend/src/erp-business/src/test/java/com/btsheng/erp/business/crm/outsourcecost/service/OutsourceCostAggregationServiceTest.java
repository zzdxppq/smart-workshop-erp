package com.btsheng.erp.business.crm.outsourcecost.service;

import com.btsheng.erp.business.crm.outsourcecost.dto.AggregateCostRequest;
import com.btsheng.erp.business.crm.outsourcecost.entity.CrmOutsourceCostAggregation;
import com.btsheng.erp.business.crm.outsourcecost.mapper.CrmOutsourceCostAggregationMapper;
import com.btsheng.erp.business.integration.client.OutsourceOrderClient;
import com.btsheng.erp.core.integration.dto.OutsourceOrderRef;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutsourceCostAggregationServiceTest {

    @Mock private CrmOutsourceCostAggregationMapper mapper;
    @Mock private OutsourceOrderClient outsourceOrderClient;

    private OutsourceCostAggregationService service;

    @BeforeEach
    void setUp() {
        service = new OutsourceCostAggregationService(mapper, outsourceOrderClient);
        when(mapper.insert(any(CrmOutsourceCostAggregation.class))).thenAnswer(inv -> {
            CrmOutsourceCostAggregation a = inv.getArgument(0);
            a.setId(1L);
            return 1;
        });
    }

    private OutsourceOrderRef mockOrder() {
        OutsourceOrderRef o = new OutsourceOrderRef();
        o.setId(1L);
        o.setOutsourceNo("WW20260612-0001");
        o.setStatus("COMPLETED");
        return o;
    }

    private void stubOrder(Long id) {
        when(outsourceOrderClient.getById(id)).thenReturn(Result.ok(mockOrder()));
    }

    private AggregateCostRequest buildValidReq() {
        AggregateCostRequest req = new AggregateCostRequest();
        req.setOutsourceId(1L);
        req.setMaterialCode("ZZ-0002");
        req.setProcessName("调质");
        req.setCostMaterial(new BigDecimal("100"));
        req.setCostLabor(new BigDecimal("30"));
        req.setCostMachine(new BigDecimal("20"));
        req.setCostOverhead(new BigDecimal("10"));
        req.setCostOutsource(new BigDecimal("150"));
        req.setBudgetCost(new BigDecimal("300"));
        req.setAggregationScope("PROCESS");
        return req;
    }

    @Test
    @DisplayName("aggregateCost happy path：5 段自动累加")
    void testAggregate_Happy() {
        stubOrder(1L);

        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(buildValidReq(), 201L);
        assertEquals(0, r.getCode());
        assertEquals(0, r.getData().getCostTotal().compareTo(new BigDecimal("310")));
    }

    @Test
    @DisplayName("aggregateCost 委外 ID 必填")
    void testAggregate_OutsourceIdRequired() {
        AggregateCostRequest req = buildValidReq();
        req.setOutsourceId(null);
        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("aggregateCost 物料编码必填")
    void testAggregate_MaterialCodeRequired() {
        AggregateCostRequest req = buildValidReq();
        req.setMaterialCode(null);
        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("aggregateCost 委外单不存在 → 40404")
    void testAggregate_OrderNotFound() {
        when(outsourceOrderClient.getById(999L)).thenReturn(Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND"));
        AggregateCostRequest req = buildValidReq();
        req.setOutsourceId(999L);
        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals(40404, r.getCode());
    }

    @Test
    @DisplayName("aggregateCost 材料成本非负：负数 → 40001")
    void testAggregate_CostNonNeg_Material() {
        AggregateCostRequest req = buildValidReq();
        req.setCostMaterial(new BigDecimal("-1"));
        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals(40001, r.getCode());
        assertEquals("COST_NON_NEGATIVE_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("aggregateCost 委外成本非负：负数 → 40001")
    void testAggregate_CostNonNeg_Outsource() {
        AggregateCostRequest req = buildValidReq();
        req.setCostOutsource(new BigDecimal("-1"));
        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("aggregateCost 偏差率 < 5% → WITHIN")
    void testAggregate_DeviationWithin() {
        stubOrder(1L);
        AggregateCostRequest req = buildValidReq();
        req.setBudgetCost(new BigDecimal("320"));
        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals("WITHIN", r.getData().getDeviationLevel());
    }

    @Test
    @DisplayName("aggregateCost 偏差率 8% → WARN")
    void testAggregate_DeviationWarn() {
        stubOrder(1L);
        AggregateCostRequest req = buildValidReq();
        req.setBudgetCost(new BigDecimal("287"));
        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals("WARN", r.getData().getDeviationLevel());
    }

    @Test
    @DisplayName("aggregateCost 偏差率 ≥ 10% → OVER")
    void testAggregate_DeviationOver() {
        stubOrder(1L);
        AggregateCostRequest req = buildValidReq();
        req.setBudgetCost(new BigDecimal("250"));
        Result<CrmOutsourceCostAggregation> r = service.aggregateCost(req, 201L);
        assertEquals("OVER", r.getData().getDeviationLevel());
    }

    @Test
    @DisplayName("getCostBySegment 返回 5 段聚合")
    void testGetSegment_OK() {
        Map<String, Object> seg = new HashMap<>();
        seg.put("sumMaterial", 100);
        seg.put("sumLabor", 30);
        seg.put("sumMachine", 20);
        seg.put("sumOverhead", 10);
        seg.put("sumOutsource", 150);
        seg.put("sumTotal", 310);
        seg.put("sumBudget", 300);
        when(mapper.selectSegmentSumByOutsourceId(1L)).thenReturn(seg);

        Result<Map<String, Object>> r = service.getCostBySegment(1L);
        assertEquals(0, r.getCode());
        assertEquals(310, r.getData().get("sumTotal"));
    }

    @Test
    @DisplayName("getCostBySegment 委外 ID 必填")
    void testGetSegment_OutsourceIdRequired() {
        Result<Map<String, Object>> r = service.getCostBySegment(null);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("exportCostReport 按委外单导出")
    void testExport_ByOutsource() {
        CrmOutsourceCostAggregation a = new CrmOutsourceCostAggregation();
        a.setId(1L);
        when(mapper.selectByOutsourceId(1L)).thenReturn(List.of(a));

        Result<List<CrmOutsourceCostAggregation>> r = service.exportCostReport(1L, null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    @Test
    @DisplayName("exportCostReport 按 scope 导出")
    void testExport_ByScope() {
        CrmOutsourceCostAggregation a = new CrmOutsourceCostAggregation();
        a.setId(1L);
        when(mapper.selectByScope("PROCESS")).thenReturn(List.of(a));

        Result<List<CrmOutsourceCostAggregation>> r = service.exportCostReport(null, "PROCESS");
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    @Test
    @DisplayName("exportCostReport 全量导出")
    void testExport_All() {
        CrmOutsourceCostAggregation a = new CrmOutsourceCostAggregation();
        a.setId(1L);
        when(mapper.selectList(null)).thenReturn(List.of(a));

        Result<List<CrmOutsourceCostAggregation>> r = service.exportCostReport(null, null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }
}
