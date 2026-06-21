package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.business.crm.quote.template.service.QuoteTemplateService;
import com.btsheng.erp.business.crm.quote.cost.service.QuoteCostCalculationService;
import com.btsheng.erp.core.model.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V2.1 · 报价工程师工艺填写 Service
 *
 * <p>工程师在报价阶段填写工序工时，系统套用范本计算报价
 */
@Service
public class QuoteProcessService {

    private final CrmQuoteMapper quoteMapper;
    private final CrmQuoteItemMapper itemMapper;
    private final QuoteTemplateService templateService;
    private final QuoteCostCalculationService costCalculationService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public QuoteProcessService(CrmQuoteMapper quoteMapper,
                             CrmQuoteItemMapper itemMapper,
                             QuoteTemplateService templateService,
                             QuoteCostCalculationService costCalculationService) {
        this.quoteMapper = quoteMapper;
        this.itemMapper = itemMapper;
        this.templateService = templateService;
        this.costCalculationService = costCalculationService;
    }

    /**
     * 工序明细（工程师填写）
     */
    public static class ProcessDetail {
        private String processCode;
        private String processName;
        private String machineType;
        private Integer unitTimeMinutes;
        private BigDecimal costPerHour;
        private Integer outsourceFlag;
        private String remark;

        public String getProcessCode() { return processCode; }
        public void setProcessCode(String processCode) { this.processCode = processCode; }
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }
        public String getMachineType() { return machineType; }
        public void setMachineType(String machineType) { this.machineType = machineType; }
        public Integer getUnitTimeMinutes() { return unitTimeMinutes; }
        public void setUnitTimeMinutes(Integer unitTimeMinutes) { this.unitTimeMinutes = unitTimeMinutes; }
        public BigDecimal getCostPerHour() { return costPerHour; }
        public void setCostPerHour(BigDecimal costPerHour) { this.costPerHour = costPerHour; }
        public Integer getOutsourceFlag() { return outsourceFlag; }
        public void setOutsourceFlag(Integer outsourceFlag) { this.outsourceFlag = outsourceFlag; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    /**
     * 工程师填写报价明细的工艺
     *
     * @param quoteItemId 报价明细ID
     * @param processes 工序列表
     * @param operatorUserId 操作人ID
     * @return 更新后的报价明细
     */
    @Transactional
    public Result<CrmQuoteItem> fillProcess(Long quoteItemId, List<ProcessDetail> processes, Long operatorUserId) {
        CrmQuoteItem item = itemMapper.selectById(quoteItemId);
        if (item == null) {
            return Result.fail(40401, "QUOTE_ITEM_NOT_FOUND");
        }

        // 1. 计算总工时
        int totalMinutes = processes.stream()
                .mapToInt(p -> p.getUnitTimeMinutes() != null ? p.getUnitTimeMinutes() : 0)
                .sum();
        BigDecimal totalHours = new BigDecimal(totalMinutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);
        item.setTotalHours(totalHours);

        // 2. 生成工艺路线描述
        String processRoute = processes.stream()
                .map(ProcessDetail::getProcessName)
                .reduce((a, b) -> a + "→" + b)
                .orElse("");
        item.setProcessRoute(processRoute);

        // 3. 序列化工序明细为JSON
        try {
            String processSummary = mapper.writeValueAsString(processes);
            item.setProcessSummary(processSummary);
        } catch (JsonProcessingException e) {
            return Result.fail(50001, "PROCESS_SERIALIZE_ERROR");
        }

        itemMapper.updateById(item);

        return Result.ok(item);
    }

    /**
     * 工程师填写多个报价明细的工艺
     *
     * @param quoteId 报价ID
     * @param itemProcessesMap 报价明细ID -> 工序列表
     * @param operatorUserId 操作人ID
     * @return 更新结果
     */
    @Transactional
    public Result<Map<String, Object>> fillProcesses(Long quoteId, Map<Long, List<ProcessDetail>> itemProcessesMap, Long operatorUserId) {
        CrmQuote quote = quoteMapper.selectById(quoteId);
        if (quote == null) {
            return Result.fail(40401, "QUOTE_NOT_FOUND");
        }

        Map<String, Object> results = new HashMap<>();
        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<Long, List<ProcessDetail>> entry : itemProcessesMap.entrySet()) {
            Result<CrmQuoteItem> result = fillProcess(entry.getKey(), entry.getValue(), operatorUserId);
            if (result.isSuccess()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        // 4. 计算报价总金额（如果所有明细都已填写工艺）
        recalculateQuoteTotal(quoteId);

        results.put("successCount", successCount);
        results.put("failCount", failCount);
        results.put("quoteId", quoteId);

        return Result.ok(results);
    }

    /**
     * 根据范本自动填充工艺
     *
     * @param quoteItemId 报价明细ID
     * @param templateId 范本ID
     * @param operatorUserId 操作人ID
     * @return 更新结果
     */
    @Transactional
    public Result<CrmQuoteItem> applyTemplate(Long quoteItemId, Long templateId, Long operatorUserId) {
        CrmQuoteItem item = itemMapper.selectById(quoteItemId);
        if (item == null) {
            return Result.fail(40401, "QUOTE_ITEM_NOT_FOUND");
        }

        // 1. 查询范本
        Result<Map<String, Object>> templateResult = templateService.getTemplateWithProcesses(templateId);
        if (!templateResult.isSuccess()) {
            return Result.fail(templateResult.getCode(), templateResult.getMessage());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> templateProcesses = (List<Map<String, Object>>) templateResult.getData().get("processes");

        // 2. 转换并保存
        try {
            @SuppressWarnings("unchecked")
            List<ProcessDetail> processes = templateProcesses.stream().map(p -> {
                ProcessDetail detail = new ProcessDetail();
                detail.setProcessCode((String) p.get("processCode"));
                detail.setProcessName((String) p.get("processName"));
                detail.setMachineType((String) p.get("machineType"));
                detail.setUnitTimeMinutes((Integer) p.get("unitTimeMinutes"));
                detail.setCostPerHour((BigDecimal) p.get("costPerHour"));
                detail.setOutsourceFlag((Integer) p.get("outsourceFlag"));
                detail.setRemark((String) p.get("remark"));
                return detail;
            }).toList();

            Result<CrmQuoteItem> fillResult = fillProcess(quoteItemId, processes, operatorUserId);
            if (fillResult.isSuccess()) {
                CrmQuoteItem updatedItem = fillResult.getData();
                updatedItem.setTemplateId(templateId);
                itemMapper.updateById(updatedItem);
            }

            return fillResult;
        } catch (Exception e) {
            return Result.fail(50001, "TEMPLATE_APPLY_ERROR");
        }
    }

    /**
     * 重新计算报价总金额
     */
    private void recalculateQuoteTotal(Long quoteId) {
        List<CrmQuoteItem> items = itemMapper.selectByQuoteId(quoteId);
        BigDecimal total = items.stream()
                .map(item -> {
                    if (item.getAmount() != null) {
                        return item.getAmount();
                    }
                    if (item.getUnitPrice() != null && item.getQuantity() != null) {
                        return item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CrmQuote quote = quoteMapper.selectById(quoteId);
        quote.setTotalAmount(total);
        quoteMapper.updateById(quote);
    }

    /**
     * 获取报价明细的工艺信息
     */
    public Result<Map<String, Object>> getProcessInfo(Long quoteItemId) {
        CrmQuoteItem item = itemMapper.selectById(quoteItemId);
        if (item == null) {
            return Result.fail(40401, "QUOTE_ITEM_NOT_FOUND");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("item", item);

        // 反序列化工艺明细
        if (item.getProcessSummary() != null && !item.getProcessSummary().isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                List<ProcessDetail> processes = mapper.readValue(item.getProcessSummary(),
                        mapper.getTypeFactory().constructCollectionType(List.class, ProcessDetail.class));
                result.put("processes", processes);
            } catch (JsonProcessingException e) {
                result.put("processes", null);
            }
        } else {
            result.put("processes", null);
        }

        return Result.ok(result);
    }

    /**
     * 自动计算报价（工程师填写工艺后）
     *
     * @param quoteItemId 报价明细ID
     * @param operatorUserId 操作人ID
     * @return 计算结果
     */
    @Transactional
    public Result<Map<String, Object>> calculateQuoteItem(Long quoteItemId, Long operatorUserId) {
        CrmQuoteItem item = itemMapper.selectById(quoteItemId);
        if (item == null) {
            return Result.fail(40401, "QUOTE_ITEM_NOT_FOUND");
        }

        Map<String, Object> result = new HashMap<>();

        // 解析工艺明细
        if (item.getProcessSummary() == null || item.getProcessSummary().isBlank()) {
            return Result.fail(40001, "PROCESS_NOT_FILLED");
        }

        try {
            @SuppressWarnings("unchecked")
            List<ProcessDetail> processes = mapper.readValue(item.getProcessSummary(),
                    mapper.getTypeFactory().constructCollectionType(List.class, ProcessDetail.class));

            // 计算总工时
            int totalMinutes = processes.stream()
                    .mapToInt(p -> p.getUnitTimeMinutes() != null ? p.getUnitTimeMinutes() : 0)
                    .sum();
            BigDecimal totalHours = new BigDecimal(totalMinutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);

            // 如果有范本ID，使用范本成本计算
            if (item.getTemplateId() != null) {
                Result<Map<String, Object>> calcResult = templateService.calculatePrice(
                        item.getTemplateId(), item.getQuantity(), item.getSurfaceArea());
                if (calcResult.isSuccess()) {
                    result.putAll(calcResult.getData());

                    // 更新报价明细
                    item.setUnitPrice((BigDecimal) calcResult.getData().get("unitPrice"));
                    item.setTotalHours(totalHours);
                    item.setAmount((BigDecimal) calcResult.getData().get("totalPrice"));
                    itemMapper.updateById(item);

                    // 更新报价总金额
                    recalculateQuoteTotal(item.getQuoteId());
                }
            } else {
                try {
                    Map<String, Object> calc = costCalculationService.calculate(item);
                    BigDecimal totalPrice = (BigDecimal) calc.get("totalPrice");
                    BigDecimal unitPrice = (BigDecimal) calc.get("unitPrice");

                    item.setUnitPrice(unitPrice);
                    item.setTotalHours(totalHours);
                    item.setAmount(totalPrice);
                    itemMapper.updateById(item);
                    recalculateQuoteTotal(item.getQuoteId());
                    checkAndMarkEngineerCompleted(item.getQuoteId());

                    result.putAll(calc);
                    result.put("totalHours", totalHours);
                } catch (Exception ex) {
                    return Result.fail(50001, "QUOTE_CALC_ERROR");
                }
            }

            result.put("itemId", item.getId());
            result.put("processCount", processes.size());

            return Result.ok(result);
        } catch (JsonProcessingException e) {
            return Result.fail(50001, "PROCESS_PARSE_ERROR");
        }
    }

    @Transactional
    public Result<CrmQuoteItem> saveSurfaceAreas(Long quoteItemId, java.math.BigDecimal anodizeArea,
                                                  java.math.BigDecimal solidSolutionArea,
                                                  java.math.BigDecimal formingArea, Long operatorUserId) {
        CrmQuoteItem item = itemMapper.selectById(quoteItemId);
        if (item == null) {
            return Result.fail(40401, "QUOTE_ITEM_NOT_FOUND");
        }
        item.setAnodizeArea(anodizeArea);
        item.setSolidSolutionArea(solidSolutionArea);
        item.setFormingArea(formingArea);
        itemMapper.updateById(item);
        return Result.ok(item);
    }

    private void checkAndMarkEngineerCompleted(Long quoteId) {
        List<CrmQuoteItem> items = itemMapper.selectByQuoteId(quoteId);
        boolean allDone = items.stream().allMatch(i ->
                i.getProcessSummary() != null && !i.getProcessSummary().isBlank()
                        && i.getUnitPrice() != null && i.getUnitPrice().compareTo(BigDecimal.ZERO) > 0);
        if (allDone) {
            CrmQuote quote = quoteMapper.selectById(quoteId);
            if (quote != null) {
                quote.setEngineerCompleted(1);
                quoteMapper.updateById(quote);
            }
        }
    }
}
