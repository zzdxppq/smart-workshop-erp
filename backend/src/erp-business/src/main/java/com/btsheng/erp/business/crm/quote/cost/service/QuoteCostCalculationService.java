package com.btsheng.erp.business.crm.quote.cost.service;

import com.btsheng.erp.business.crm.quote.cost.entity.CrmQuoteCostItem;
import com.btsheng.erp.business.crm.quote.cost.mapper.CrmQuoteCostItemMapper;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.service.QuoteProcessService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * V2.1 · 套用报价成本项目录计算报价明细
 */
@Service
public class QuoteCostCalculationService {

    private final CrmQuoteCostItemMapper costItemMapper;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public QuoteCostCalculationService(CrmQuoteCostItemMapper costItemMapper) {
        this.costItemMapper = costItemMapper;
    }

    public Map<String, Object> calculate(CrmQuoteItem item) throws Exception {
        List<CrmQuoteCostItem> costItems = costItemMapper.selectAllActive();
        Map<String, CrmQuoteCostItem> byCode = costItems.stream()
                .collect(Collectors.toMap(CrmQuoteCostItem::getItemCode, c -> c, (a, b) -> a));
        Map<String, CrmQuoteCostItem> byProcess = costItems.stream()
                .filter(c -> c.getProcessCode() != null && !c.getProcessCode().isBlank())
                .collect(Collectors.toMap(CrmQuoteCostItem::getProcessCode, c -> c, (a, b) -> a));

        BigDecimal subtotal = BigDecimal.ZERO;
        Map<String, BigDecimal> breakdown = new HashMap<>();

        subtotal = addWeightCost(breakdown, subtotal, byCode.get("MATERIAL"), item.getUnitWeight(), item.getQuantity());

        if (item.getProcessSummary() != null && !item.getProcessSummary().isBlank()) {
            List<QuoteProcessService.ProcessDetail> processes = mapper.readValue(
                    item.getProcessSummary(),
                    new TypeReference<List<QuoteProcessService.ProcessDetail>>() {});
            for (QuoteProcessService.ProcessDetail p : processes) {
                CrmQuoteCostItem ci = byProcess.get(p.getProcessCode());
                if (ci == null || ci.getUnitPrice() == null) continue;
                int minutes = p.getUnitTimeMinutes() != null ? p.getUnitTimeMinutes() : 0;
                BigDecimal hours = new BigDecimal(minutes).divide(new BigDecimal(60), 4, RoundingMode.HALF_UP);
                BigDecimal line = costWithMargin(hours.multiply(ci.getUnitPrice()), ci.getProfitMargin());
                breakdown.merge(ci.getItemCode(), line, BigDecimal::add);
                subtotal = subtotal.add(line);
            }
        }

        subtotal = addAreaCost(breakdown, subtotal, byCode.get("ANODIZE"), item.getAnodizeArea());
        subtotal = addAreaCost(breakdown, subtotal, byCode.get("SOLID_SOLUTION"), item.getSolidSolutionArea());
        subtotal = addAreaCost(breakdown, subtotal, byCode.get("FORMING"), item.getFormingArea());

        CrmQuoteCostItem express = byCode.get("EXPRESS");
        if (express != null && express.getUnitPrice() != null
                && item.getUnitWeight() != null && item.getQuantity() != null) {
            BigDecimal weight = item.getUnitWeight().multiply(new BigDecimal(item.getQuantity()));
            BigDecimal line = costWithMargin(weight.multiply(express.getUnitPrice()), express.getProfitMargin());
            breakdown.put("EXPRESS", line);
            subtotal = subtotal.add(line);
        }

        CrmQuoteCostItem sgna = byCode.get("SGNA");
        if (sgna != null && sgna.getProfitMargin() != null) {
            BigDecimal sgnaCost = subtotal.multiply(sgna.getProfitMargin()).setScale(4, RoundingMode.HALF_UP);
            breakdown.put("SGNA", sgnaCost);
            subtotal = subtotal.add(sgnaCost);
        }

        int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
        BigDecimal unitPrice = subtotal.divide(new BigDecimal(qty), 2, RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("breakdown", breakdown);
        result.put("totalPrice", subtotal.setScale(2, RoundingMode.HALF_UP));
        result.put("unitPrice", unitPrice);
        result.put("quantity", qty);
        return result;
    }

    private BigDecimal addWeightCost(Map<String, BigDecimal> breakdown, BigDecimal subtotal,
                                     CrmQuoteCostItem ci, BigDecimal unitWeight, Integer qty) {
        if (ci == null || ci.getUnitPrice() == null || unitWeight == null || qty == null) {
            return subtotal;
        }
        BigDecimal weight = unitWeight.multiply(new BigDecimal(qty));
        BigDecimal line = costWithMargin(weight.multiply(ci.getUnitPrice()), ci.getProfitMargin());
        breakdown.put(ci.getItemCode(), line);
        return subtotal.add(line);
    }

    private BigDecimal addAreaCost(Map<String, BigDecimal> breakdown, BigDecimal subtotal,
                                   CrmQuoteCostItem ci, BigDecimal area) {
        if (ci == null || ci.getUnitPrice() == null || area == null || area.compareTo(BigDecimal.ZERO) <= 0) {
            return subtotal;
        }
        BigDecimal line = costWithMargin(area.multiply(ci.getUnitPrice()), ci.getProfitMargin());
        breakdown.put(ci.getItemCode(), line);
        return subtotal.add(line);
    }

    private BigDecimal costWithMargin(BigDecimal base, BigDecimal margin) {
        if (base == null) return BigDecimal.ZERO;
        BigDecimal m = margin != null ? margin : BigDecimal.ZERO;
        return base.multiply(BigDecimal.ONE.add(m)).setScale(4, RoundingMode.HALF_UP);
    }
}
