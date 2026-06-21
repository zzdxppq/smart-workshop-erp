package com.btsheng.erp.business.finance.cost.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.cost.dto.AggregateCostRequest;
import com.btsheng.erp.business.finance.cost.entity.CrmCostAccounting;
import com.btsheng.erp.business.finance.cost.entity.CrmCostSegment;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostAccountingMapper;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostSegmentMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.37 · 财务·成本核算 Service (FR-9-2)
 *
 * <p>4 业务方法：aggregateCost / getCostBySegment / getCostByOrder / listCosts
 * <p>3 P1 修补�? 段自动归�?/ 成本非负 / 偏差率统�? * <p>5 段：MATERIAL（材�?1.9/1.17�? PROCESS（加�?1.10�? OUTSOURCE（委�?1.26�? MANAGE（管�?1.17�? DEPRECIATION（折�?1.9�? */
@Service
public class CostAccountingService {

    public static final String SEG_MATERIAL = "MATERIAL";
    public static final String SEG_PROCESS = "PROCESS";
    public static final String SEG_OUTSOURCE = "OUTSOURCE";
    public static final String SEG_MANAGE = "MANAGE";
    public static final String SEG_DEPRECIATION = "DEPRECIATION";

    public static final String REF_ORDER = "ORDER";
    public static final String REF_WORKORDER = "WORKORDER";
    public static final String REF_OUTSOURCE = "OUTSOURCE";

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_CONFIRMED = "CONFIRMED";

    private final CrmCostAccountingMapper accountingMapper;
    private final CrmCostSegmentMapper segmentMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public CostAccountingService(CrmCostAccountingMapper accountingMapper,
                                 CrmCostSegmentMapper segmentMapper,
                                 DocNoGenerator docNoGenerator) {
        this.accountingMapper = accountingMapper;
        this.segmentMapper = segmentMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-9.2.1�? 段成本自动归�?     * P1 修补 2：各段成本非�?     * P1 修补 3：偏差率统计
     */
    @Transactional
    @AuditLog(module = "cost_accounting", action = "cost.aggregate")
    public Result<Map<String, Object>> aggregateCost(AggregateCostRequest req, Long operatorUserId) {
        if (req == null || req.getRefType() == null || req.getRefId() == null) {
            return Result.fail(40001, "REF_REQUIRED");
        }
        if (req.getQty() == null || req.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "QTY_INVALID");
        }
        if (req.getCostDate() == null) {
            return Result.fail(40001, "COST_DATE_REQUIRED");
        }
        // P1 修补 2：各段非�?
            BigDecimal[] amts = new BigDecimal[]{
                req.getMaterialAmount(), req.getProcessAmount(), req.getOutsourceAmount(),
                req.getManageAmount(), req.getDepreciationAmount()};
        for (BigDecimal a : amts) {
            if (a == null || a.compareTo(BigDecimal.ZERO) < 0) {
                return Result.fail(40001, "SEGMENT_AMOUNT_NEGATIVE");
            }
        }
        if (accountingMapper.selectByRef(req.getRefType(), req.getRefId()) != null) {
            return Result.fail(40902, "COST_DUPLICATE");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal a : amts) total = total.add(a);
        BigDecimal unitCost = total.divide(req.getQty(), 4, RoundingMode.HALF_UP);

        CrmCostAccounting c = new CrmCostAccounting();
        c.setCostNo(docNoGenerator.nextCostAccountingNo());
        c.setRefType(req.getRefType());
        c.setRefId(req.getRefId());
        c.setRefNo(req.getRefNo());
        c.setMaterialId(req.getMaterialId());
        c.setMaterialCode(req.getMaterialCode());
        c.setMaterialName(req.getMaterialName());
        c.setQty(req.getQty());
        c.setUnitCost(unitCost);
        c.setTotalCost(total);
        c.setStandardCost(req.getStandardCost());
        // P1 修补 3：偏�?
            if (req.getStandardCost() != null) {
            BigDecimal variance = total.subtract(req.getStandardCost());
            c.setVariance(variance);
            if (req.getStandardCost().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal rate = variance.multiply(new BigDecimal(100))
                        .divide(req.getStandardCost(), 4, RoundingMode.HALF_UP);
                c.setVarianceRate(rate);
            }
        }
        c.setStatus(STATUS_DRAFT);
        c.setCostDate(req.getCostDate());
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        accountingMapper.insert(c);

        // 5 段明�?
            String[] codes = new String[]{SEG_MATERIAL, SEG_PROCESS, SEG_OUTSOURCE, SEG_MANAGE, SEG_DEPRECIATION};
        String[] names = new String[]{"材料", "加工", "委外", "管理", "折旧"};
        String[] sources = new String[]{"1.9/1.17", "1.10", "1.26", "1.17", "1.9"};
        for (int i = 0; i < 5; i++) {
            CrmCostSegment s = new CrmCostSegment();
            s.setCostId(c.getId());
            s.setSegmentCode(codes[i]);
            s.setSegmentName(names[i]);
            s.setAmount(amts[i]);
            s.setSource(sources[i]);
            segmentMapper.insert(s);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("accounting", c);
        data.put("segments", segmentMapper.selectByCostId(c.getId()));
        return Result.ok(data);
    }

    /**
     * 按段聚合（跨所有核算单�?     */
    @AuditLog(module = "cost_accounting", action = "cost.by_segment")
    public Result<Map<String, Object>> getCostBySegment() {
        List<CrmCostAccounting> all = accountingMapper.selectAll();
        Map<String, BigDecimal> bySeg = new HashMap<>();
        for (String code : new String[]{SEG_MATERIAL, SEG_PROCESS, SEG_OUTSOURCE, SEG_MANAGE, SEG_DEPRECIATION}) {
            bySeg.put(code, BigDecimal.ZERO);
        }
        if (all != null) {
            for (CrmCostAccounting c : all) {
                List<CrmCostSegment> segs = segmentMapper.selectByCostId(c.getId());
                if (segs != null) {
                    for (CrmCostSegment s : segs) {
                        bySeg.merge(s.getSegmentCode(), s.getAmount(), BigDecimal::add);
                    }
                }
            }
        }
        Map<String, Object> out = new HashMap<>();
        out.put("by_segment", bySeg);
        return Result.ok(out);
    }

    /**
     * �?ref（订�?工单/委外）取核算�?     */
    @AuditLog(module = "cost_accounting", action = "cost.by_order")
    public Result<Map<String, Object>> getCostByOrder(String refType, Long refId) {
        if (refType == null || refId == null) return Result.fail(40001, "REF_REQUIRED");
        CrmCostAccounting c = accountingMapper.selectByRef(refType, refId);
        if (c == null) return Result.fail(40404, "COST_NOT_FOUND");
        Map<String, Object> out = new HashMap<>();
        out.put("accounting", c);
        out.put("segments", segmentMapper.selectByCostId(c.getId()));
        return Result.ok(out);
    }

    /**
     * 列表（按 refType 过滤�?     */
    @AuditLog(module = "cost_accounting", action = "cost.list")
    public Result<List<Map<String, Object>>> listCosts(String refType) {
        List<CrmCostAccounting> all = refType == null
                ? accountingMapper.selectAll()
                : accountingMapper.selectByRefType(refType);
        List<Map<String, Object>> out = new ArrayList<>();
        if (all == null) return Result.ok(out);
        for (CrmCostAccounting c : all) {
            Map<String, Object> e = new HashMap<>();
            e.put("accounting", c);
            e.put("segments", segmentMapper.selectByCostId(c.getId()));
            out.add(e);
        }
        return Result.ok(out);
    }
}
