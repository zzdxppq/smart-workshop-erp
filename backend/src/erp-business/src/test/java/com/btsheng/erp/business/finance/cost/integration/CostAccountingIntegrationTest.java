package com.btsheng.erp.business.finance.cost.integration;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.cost.dto.AggregateCostRequest;
import com.btsheng.erp.business.finance.cost.entity.CrmCostAccounting;
import com.btsheng.erp.business.finance.cost.entity.CrmCostSegment;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostAccountingMapper;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostSegmentMapper;
import com.btsheng.erp.business.finance.cost.service.CostAccountingService;
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
 * V1.3.7 · Story 1.37 · 财务·成本核算 集成测试（FR-9-2）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CostAccountingIntegrationTest {

    @Mock private CrmCostAccountingMapper accountingMapper;
    @Mock private CrmCostSegmentMapper segmentMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private CostAccountingService service;

    @BeforeEach
    void setUp() {
        service = new CostAccountingService(accountingMapper, segmentMapper, docNoGenerator);
        when(docNoGenerator.nextCostAccountingNo())
                .thenReturn("CA20260612-0001", "CA20260612-0002", "CA20260612-0003", "CA20260612-0004");
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

    private AggregateCostRequest build(String refType, long refId, String refNo,
                                      String material, String process, String outsource,
                                      String manage, String deprec) {
        AggregateCostRequest r = new AggregateCostRequest();
        r.setRefType(refType);
        r.setRefId(refId);
        r.setRefNo(refNo);
        r.setQty(new BigDecimal("100"));
        r.setStandardCost(new BigDecimal("100000"));
        r.setCostDate(LocalDate.now());
        r.setMaterialAmount(new BigDecimal(material));
        r.setProcessAmount(new BigDecimal(process));
        r.setOutsourceAmount(new BigDecimal(outsource));
        r.setManageAmount(new BigDecimal(manage));
        r.setDepreciationAmount(new BigDecimal(deprec));
        return r;
    }

    // ====== lifecycle 1：ORDER 5 段归集 ======
            @Test
    @DisplayName("lifecycle 1：ORDER 5 段归集 → 偏差率")
    void testIntegration_Order() {
        when(accountingMapper.selectByRef(any(), any())).thenReturn(null);
        AggregateCostRequest r = build("ORDER", 5001L, "XS20260501-0001",
                "40000", "25000", "10000", "6000", "4000");
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        assertEquals(0, res.getCode());
        CrmCostAccounting c = (CrmCostAccounting) res.getData().get("accounting");
        assertEquals(0, new BigDecimal("85000").compareTo(c.getTotalCost()));
        // variance = 85000 - 100000 = -15000
            assertEquals(0, new BigDecimal("-15000").compareTo(c.getVariance()));
        // rate = -15000/100000 * 100 = -15
            assertEquals(0, new BigDecimal("-15.0000").compareTo(c.getVarianceRate()));
    }

    // ====== lifecycle 2：WORKORDER 归集 ======
            @Test
    @DisplayName("lifecycle 2：WORKORDER 归集")
    void testIntegration_WorkOrder() {
        when(accountingMapper.selectByRef(any(), any())).thenReturn(null);
        AggregateCostRequest r = build("WORKORDER", 6001L, "GD20260501-0001",
                "20000", "15000", "5000", "3000", "2000");
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        assertEquals(0, res.getCode());
        CrmCostAccounting c = (CrmCostAccounting) res.getData().get("accounting");
        assertEquals("WORKORDER", c.getRefType());
    }

    // ====== lifecycle 3：OUTSOURCE 归集 ======
            @Test
    @DisplayName("lifecycle 3：OUTSOURCE 归集")
    void testIntegration_Outsource() {
        when(accountingMapper.selectByRef(any(), any())).thenReturn(null);
        AggregateCostRequest r = build("OUTSOURCE", 7001L, "WW20260515-0001",
                "30000", "20000", "40000", "4000", "2000");
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        assertEquals(0, res.getCode());
        CrmCostAccounting c = (CrmCostAccounting) res.getData().get("accounting");
        assertEquals("OUTSOURCE", c.getRefType());
        // 委外段 40000 是最大段
    }

    // ====== AC-9.2.1：单号模板 ======
            @Test
    @DisplayName("AC-9.2.1：单号模板 CA{yyyyMMdd}{seq:4}")
    void testIntegration_CaNo() {
        when(accountingMapper.selectByRef(any(), any())).thenReturn(null);
        AggregateCostRequest r = build("ORDER", 9999L, "XS9999",
                "1000", "1000", "0", "500", "500");
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        CrmCostAccounting c = (CrmCostAccounting) res.getData().get("accounting");
        assertTrue(c.getCostNo().startsWith("CA"));
        assertEquals(15, c.getCostNo().length());
    }

    // ====== 5 段全 0 ======
            @Test
    @DisplayName("5 段全 0 合法 · total=0")
    void testIntegration_AllZero() {
        when(accountingMapper.selectByRef(any(), any())).thenReturn(null);
        AggregateCostRequest r = build("ORDER", 8001L, "XS8001",
                "0", "0", "0", "0", "0");
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        assertEquals(0, res.getCode());
        CrmCostAccounting c = (CrmCostAccounting) res.getData().get("accounting");
        assertEquals(0, new BigDecimal("0").compareTo(c.getTotalCost()));
    }

    // ====== 仅委外段非 0（无材料/加工） ======
            @Test
    @DisplayName("P1 修补 1：委外占比 80% 的归集")
    void testIntegration_OutsourceHeavy() {
        when(accountingMapper.selectByRef(any(), any())).thenReturn(null);
        AggregateCostRequest r = build("OUTSOURCE", 8002L, "WW8002",
                "1000", "1000", "8000", "0", "0");
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        CrmCostAccounting c = (CrmCostAccounting) res.getData().get("accounting");
        // total = 10000
            assertEquals(0, new BigDecimal("10000").compareTo(c.getTotalCost()));
    }

    // ====== 跨 1.6 订单 ======
            @Test
    @DisplayName("跨 1.6：ORDER 关联（refType=ORDER）")
    void testIntegration_CrossOrder() {
        when(accountingMapper.selectByRef(any(), any())).thenReturn(null);
        AggregateCostRequest r = build("ORDER", 5005L, "XS20260515-0005",
                "10000", "5000", "0", "2000", "1000");
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        CrmCostAccounting c = (CrmCostAccounting) res.getData().get("accounting");
        assertEquals("XS20260515-0005", c.getRefNo());
    }

    // ====== 偏差 0 ======
            @Test
    @DisplayName("无偏差：total = standard → variance=0")
    void testIntegration_NoVariance() {
        when(accountingMapper.selectByRef(any(), any())).thenReturn(null);
        AggregateCostRequest r = build("ORDER", 5010L, "XS5010",
                "5000", "3000", "1000", "500", "500");
        r.setStandardCost(new BigDecimal("10000"));
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        CrmCostAccounting c = (CrmCostAccounting) res.getData().get("accounting");
        assertEquals(0, new BigDecimal("0").compareTo(c.getVariance()));
        assertEquals(0, new BigDecimal("0").compareTo(c.getVarianceRate()));
    }

    // ====== 重复拦截 ======
            @Test
    @DisplayName("refType+refId 重复 → 40902")
    void testIntegration_Duplicate() {
        CrmCostAccounting existed = new CrmCostAccounting();
        existed.setId(99L);
        when(accountingMapper.selectByRef("ORDER", 5001L)).thenReturn(existed);
        AggregateCostRequest r = build("ORDER", 5001L, "XS5001",
                "100", "100", "0", "0", "0");
        Result<Map<String, Object>> res = service.aggregateCost(r, 703L);
        assertEquals(40902, res.getCode());
    }

    // ====== getCostByOrder 集成 ======
            @Test
    @DisplayName("getCostByOrder · 5 段回写")
    void testIntegration_ByOrder() {
        CrmCostAccounting c = new CrmCostAccounting();
        c.setId(1L); c.setRefType("ORDER"); c.setRefId(5001L);
        c.setTotalCost(new BigDecimal("85000"));
        when(accountingMapper.selectByRef("ORDER", 5001L)).thenReturn(c);
        when(segmentMapper.selectByCostId(1L)).thenReturn(List.of(
                seg("MATERIAL", "40000"),
                seg("PROCESS", "25000"),
                seg("OUTSOURCE", "10000"),
                seg("MANAGE", "6000"),
                seg("DEPRECIATION", "4000")
        ));
        Result<Map<String, Object>> r = service.getCostByOrder("ORDER", 5001L);
        assertEquals(0, r.getCode());
        @SuppressWarnings("unchecked")
        List<CrmCostSegment> segs = (List<CrmCostSegment>) r.getData().get("segments");
        assertEquals(5, segs.size());
    }

    private CrmCostSegment seg(String code, String amount) {
        CrmCostSegment s = new CrmCostSegment();
        s.setSegmentCode(code);
        s.setAmount(new BigDecimal(amount));
        return s;
    }
}
