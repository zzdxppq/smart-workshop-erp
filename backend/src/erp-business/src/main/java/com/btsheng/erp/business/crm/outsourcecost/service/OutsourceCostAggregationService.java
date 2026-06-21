package com.btsheng.erp.business.crm.outsourcecost.service;

import com.btsheng.erp.business.crm.outsourcecost.dto.AggregateCostRequest;
import com.btsheng.erp.business.crm.outsourcecost.entity.CrmOutsourceCostAggregation;
import com.btsheng.erp.business.crm.outsourcecost.mapper.CrmOutsourceCostAggregationMapper;
import com.btsheng.erp.business.integration.client.OutsourceOrderClient;
import com.btsheng.erp.core.integration.dto.OutsourceOrderRef;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.26 · 委外成本归集 Service (FR-6-6)
 */
@Service
public class OutsourceCostAggregationService {

    public static final String SCOPE_STEP = "STEP";
    public static final String SCOPE_PROCESS = "PROCESS";
    public static final String SCOPE_WHOLE = "WHOLE";

    public static final String DEVIATION_WITHIN = "WITHIN";
    public static final String DEVIATION_WARN = "WARN";
    public static final String DEVIATION_OVER = "OVER";

    public static final BigDecimal DEVIATION_WARN_PCT = new BigDecimal("5.00");
    public static final BigDecimal DEVIATION_OVER_PCT = new BigDecimal("10.00");

    private final CrmOutsourceCostAggregationMapper mapper;
    private final OutsourceOrderClient outsourceOrderClient;

    @Autowired
    public OutsourceCostAggregationService(CrmOutsourceCostAggregationMapper mapper,
                                            OutsourceOrderClient outsourceOrderClient) {
        this.mapper = mapper;
        this.outsourceOrderClient = outsourceOrderClient;
    }

    @Transactional
    @AuditLog(module = "outsource_cost", action = "outsource_cost.aggregate")
    public Result<CrmOutsourceCostAggregation> aggregateCost(AggregateCostRequest req, Long operatorUserId) {
        if (req == null || req.getOutsourceId() == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        if (req.getMaterialCode() == null || req.getMaterialCode().isEmpty()) {
            return Result.fail(40001, "MATERIAL_CODE_REQUIRED");
        }
        BigDecimal[] segs = new BigDecimal[]{
                req.getCostMaterial(), req.getCostLabor(), req.getCostMachine(),
                req.getCostOverhead(), req.getCostOutsource()
        };
        for (BigDecimal s : segs) {
            if (s == null || s.signum() < 0) {
                return Result.fail(40001, "COST_NON_NEGATIVE_REQUIRED");
            }
        }
        if (req.getBudgetCost() == null || req.getBudgetCost().signum() < 0) {
            return Result.fail(40001, "BUDGET_COST_NON_NEGATIVE_REQUIRED");
        }

        Result<OutsourceOrderRef> orderResult = outsourceOrderClient.getById(req.getOutsourceId());
        if (orderResult == null || orderResult.getCode() != 0 || orderResult.getData() == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        OutsourceOrderRef order = orderResult.getData();

        BigDecimal total = segs[0].add(segs[1]).add(segs[2]).add(segs[3]).add(segs[4]);

        BigDecimal deviationPct = BigDecimal.ZERO;
        if (req.getBudgetCost().signum() > 0) {
            deviationPct = total.subtract(req.getBudgetCost())
                    .multiply(new BigDecimal("100"))
                    .divide(req.getBudgetCost(), 2, RoundingMode.HALF_UP);
        }
        String deviationLevel = computeDeviationLevel(deviationPct);

        CrmOutsourceCostAggregation agg = new CrmOutsourceCostAggregation();
        agg.setOutsourceId(order.getId());
        agg.setOutsourceNo(order.getOutsourceNo());
        agg.setMaterialCode(req.getMaterialCode());
        agg.setProcessName(req.getProcessName());
        agg.setCostMaterial(req.getCostMaterial());
        agg.setCostLabor(req.getCostLabor());
        agg.setCostMachine(req.getCostMachine());
        agg.setCostOverhead(req.getCostOverhead());
        agg.setCostOutsource(req.getCostOutsource());
        agg.setCostTotal(total);
        agg.setBudgetCost(req.getBudgetCost());
        agg.setDeviationPct(deviationPct);
        agg.setDeviationLevel(deviationLevel);
        agg.setAggregationScope(req.getAggregationScope() == null ? SCOPE_PROCESS : req.getAggregationScope());
        agg.setCreatedBy(operatorUserId);
        agg.setCreatedAt(LocalDateTime.now());
        agg.setUpdatedAt(LocalDateTime.now());
        mapper.insert(agg);

        return Result.ok(agg);
    }

    @AuditLog(module = "outsource_cost", action = "outsource_cost.get_segment")
    public Result<Map<String, Object>> getCostBySegment(Long outsourceId) {
        if (outsourceId == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        Map<String, Object> seg = mapper.selectSegmentSumByOutsourceId(outsourceId);
        if (seg == null) {
            return Result.ok(new HashMap<>());
        }
        return Result.ok(seg);
    }

    @AuditLog(module = "outsource_cost", action = "outsource_cost.export")
    public Result<List<CrmOutsourceCostAggregation>> exportCostReport(Long outsourceId, String scope) {
        List<CrmOutsourceCostAggregation> list;
        if (outsourceId != null) {
            list = mapper.selectByOutsourceId(outsourceId);
        } else if (scope != null && !scope.isEmpty()) {
            list = mapper.selectByScope(scope);
        } else {
            list = mapper.selectList(null);
        }
        return Result.ok(list);
    }

    private String computeDeviationLevel(BigDecimal deviationPct) {
        BigDecimal abs = deviationPct.abs();
        if (abs.compareTo(DEVIATION_OVER_PCT) >= 0) return DEVIATION_OVER;
        if (abs.compareTo(DEVIATION_WARN_PCT) >= 0) return DEVIATION_WARN;
        return DEVIATION_WITHIN;
    }
}
