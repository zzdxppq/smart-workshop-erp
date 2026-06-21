package com.btsheng.erp.business.finance.cost.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.cost.dto.AggregateCostRequest;
import com.btsheng.erp.business.finance.cost.entity.CrmCostAccounting;
import com.btsheng.erp.business.finance.cost.entity.CrmCostSegment;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostAccountingMapper;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostSegmentMapper;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * V1.3.7 · Story 1.37 · 财务·成本核算 Service 单元测试（FR-9-2）
 * 14 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CostAccountingServiceTest {

    @Mock private CrmCostAccountingMapper accountingMapper;
    @Mock private CrmCostSegmentMapper segmentMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private CostAccountingService service;

    @BeforeEach
    void setUp() {
        service = new CostAccountingService(accountingMapper, segmentMapper, docNoGenerator);
        when(docNoGenerator.nextCostAccountingNo())
                .thenReturn("CA20260612-0001", "CA20260612-0002", "CA20260612-0003");
        when(accountingMapper.insert(any(CrmCostAccounting.class))).thenAnswer(inv -> {
            CrmCostAccounting c = inv.getArgument(0);
            c.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(segmentMapper.insert(any(CrmCostSegment.class))).thenAnswer(inv -> {
            CrmCostSegment s = inv.getArgument(0);
            s.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(segmentMapper.selectByCostId(any())).thenReturn(new ArrayList<>());
        when(accountingMapper.updateById(any(CrmCostAccounting.class))).thenReturn(1);
    }

    private AggregateCostRequest buildValid() {
        AggregateCostRequest r = new AggregateCostRequest();
        r.setRefType("ORDER");
        r.setRefId(5001L);
        r.setRefNo("XS20260501-0001");
        r.setMaterialId(2001L);
        r.setMaterialCode("M-AUTO-001");
        r.setQty(new BigDecimal("100"));
        r.setStandardCost(new BigDecimal("80000"));
        r.setCostDate(LocalDate.now());
        r.setMaterialAmount(new BigDecimal("40000"));
        r.setProcessAmount(new BigDecimal("25000"));
        r.setOutsourceAmount(new BigDecimal("10000"));
        r.setManageAmount(new BigDecimal("6000"));
        r.setDepreciationAmount(new BigDecimal("4000"));
        return r;
    }

    // ====== aggregateCost 7 测例 ======
            @Test
    @DisplayName("aggregate happy path · 5 段归集 + 单号 CA 前缀")
    void testAggregate_OK() {
        when(accountingMapper.selectByRef(any(), any())).thenReturn(null);
        Result<Map<String, Object>> r = service.aggregateCost(buildValid(), 703L);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().get("accounting").toString().contains("CA20260612-0001"));
        // total = 40000+25000+10000+6000+4000 = 85000
            CrmCostAccounting c = (CrmCostAccounting) r.getData().get("accounting");
        assertEquals(0, new BigDecimal("85000").compareTo(c.getTotalCost()));
        // unit = 85000/100 = 850
            assertEquals(0, new BigDecimal("850.0000").compareTo(c.getUnitCost()));
    }

    @Test
    @DisplayName("P1 修补 3：偏差率计算")
    void testAggregate_Variance() {
        when(accountingMapper.selectByRef(any(), any())).thenReturn(null);
        // total=85000, standard=80000 → variance=5000, rate=6.25%
            Result<Map<String, Object>> r = service.aggregateCost(buildValid(), 703L);
        CrmCostAccounting c = (CrmCostAccounting) r.getData().get("accounting");
        assertEquals(0, new BigDecimal("5000").compareTo(c.getVariance()));
        assertEquals(0, new BigDecimal("6.2500").compareTo(c.getVarianceRate()));
    }

    @Test
    @DisplayName("P1 修补 2：材料段为负 → 40001")
    void testAggregate_MaterialNegative() {
        AggregateCostRequest r = buildValid();
        r.setMaterialAmount(new BigDecimal("-1"));
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        assertEquals(40001, res.getCode());
        assertEquals("SEGMENT_AMOUNT_NEGATIVE", res.getMessage());
    }

    @Test
    @DisplayName("P1 修补 2：加工段为负 → 40001")
    void testAggregate_ProcessNegative() {
        AggregateCostRequest r = buildValid();
        r.setProcessAmount(new BigDecimal("-1"));
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("refType/refId 重复 → 40902")
    void testAggregate_Duplicate() {
        CrmCostAccounting existed = new CrmCostAccounting();
        existed.setId(99L);
        when(accountingMapper.selectByRef("ORDER", 5001L)).thenReturn(existed);
        Result<Map<String, Object>> r = service.aggregateCost(buildValid(), 703L);
        assertEquals(40902, r.getCode());
    }

    @Test
    @DisplayName("缺 refType → 40001")
    void testAggregate_NoRef() {
        AggregateCostRequest r = buildValid();
        r.setRefType(null);
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("缺成本日期 → 40001")
    void testAggregate_NoDate() {
        AggregateCostRequest r = buildValid();
        r.setCostDate(null);
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        assertEquals(40001, res.getCode());
    }

    // ====== getCostBySegment 3 测例 ======
            @Test
    @DisplayName("getCostBySegment · 5 段总金额")
    void testBySegment_OK() {
        CrmCostAccounting c = new CrmCostAccounting();
        c.setId(1L);
        c.setTotalCost(new BigDecimal("85000"));
        when(accountingMapper.selectAll()).thenReturn(List.of(c));
        when(segmentMapper.selectByCostId(1L)).thenReturn(List.of(
                seg(SEG.MATERIAL, "40000"),
                seg(SEG.PROCESS, "25000"),
                seg(SEG.OUTSOURCE, "10000"),
                seg(SEG.MANAGE, "6000"),
                seg(SEG.DEPRECIATION, "4000")
        ));
        Result<Map<String, Object>> r = service.getCostBySegment();
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> m = (Map<String, BigDecimal>) r.getData().get("by_segment");
        assertEquals(0, new BigDecimal("40000").compareTo(m.get("MATERIAL")));
        assertEquals(0, new BigDecimal("4000").compareTo(m.get("DEPRECIATION")));
    }

    @Test
    @DisplayName("getCostBySegment · 多单累加")
    void testBySegment_Multi() {
        CrmCostAccounting c1 = new CrmCostAccounting();
        c1.setId(1L);
        CrmCostAccounting c2 = new CrmCostAccounting();
        c2.setId(2L);
        when(accountingMapper.selectAll()).thenReturn(List.of(c1, c2));
        when(segmentMapper.selectByCostId(1L)).thenReturn(List.of(seg(SEG.MATERIAL, "100")));
        when(segmentMapper.selectByCostId(2L)).thenReturn(List.of(seg(SEG.MATERIAL, "200")));
        Result<Map<String, Object>> r = service.getCostBySegment();
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> m = (Map<String, BigDecimal>) r.getData().get("by_segment");
        assertEquals(0, new BigDecimal("300").compareTo(m.get("MATERIAL")));
    }

    @Test
    @DisplayName("getCostBySegment · 空数据返回 0")
    void testBySegment_Empty() {
        when(accountingMapper.selectAll()).thenReturn(new ArrayList<>());
        Result<Map<String, Object>> r = service.getCostBySegment();
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> m = (Map<String, BigDecimal>) r.getData().get("by_segment");
        assertEquals(0, new BigDecimal("0").compareTo(m.get("MATERIAL")));
    }

    // ====== getCostByOrder 2 测例 ======
            @Test
    @DisplayName("getCostByOrder · 按订单查得")
    void testByOrder_OK() {
        CrmCostAccounting c = new CrmCostAccounting();
        c.setId(1L);
        c.setRefType("ORDER");
        c.setRefId(5001L);
        when(accountingMapper.selectByRef("ORDER", 5001L)).thenReturn(c);
        when(segmentMapper.selectByCostId(1L)).thenReturn(List.of(seg(SEG.MATERIAL, "40000")));
        Result<Map<String, Object>> r = service.getCostByOrder("ORDER", 5001L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("getCostByOrder · 找不到 → 40404")
    void testByOrder_NotFound() {
        when(accountingMapper.selectByRef("ORDER", 9999L)).thenReturn(null);
        Result<Map<String, Object>> r = service.getCostByOrder("ORDER", 9999L);
        assertEquals(40404, r.getCode());
    }

    // ====== listCosts 2 测例 ======
            @Test
    @DisplayName("listCosts · 不带 refType 返回全部")
    void testList_All() {
        CrmCostAccounting c = new CrmCostAccounting();
        c.setId(1L);
        when(accountingMapper.selectAll()).thenReturn(List.of(c));
        Result<List<Map<String, Object>>> r = service.listCosts(null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    @Test
    @DisplayName("listCosts · refType 过滤")
    void testList_ByRefType() {
        CrmCostAccounting c = new CrmCostAccounting();
        c.setId(1L);
        c.setRefType("WORKORDER");
        when(accountingMapper.selectByRefType("WORKORDER")).thenReturn(List.of(c));
        Result<List<Map<String, Object>>> r = service.listCosts("WORKORDER");
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    enum SEG { MATERIAL, PROCESS, OUTSOURCE, MANAGE, DEPRECIATION }

    private CrmCostSegment seg(SEG code, String amount) {
        CrmCostSegment s = new CrmCostSegment();
        s.setSegmentCode(code.name());
        s.setAmount(new BigDecimal(amount));
        return s;
    }
}
