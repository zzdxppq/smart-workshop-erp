package com.btsheng.erp.business.crm.quote.template.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.quote.template.entity.CrmQuoteTemplate;
import com.btsheng.erp.business.crm.quote.template.entity.CrmQuoteTemplateProcess;
import com.btsheng.erp.business.crm.quote.template.mapper.CrmQuoteTemplateMapper;
import com.btsheng.erp.business.crm.quote.template.mapper.CrmQuoteTemplateProcessMapper;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * V2.1 · 报价范本 Service
 */
@Service
public class QuoteTemplateService {

    private final CrmQuoteTemplateMapper templateMapper;
    private final CrmQuoteTemplateProcessMapper processMapper;
    private final DocNoGenerator docNoGenerator;
    private final AtomicLong seqCounter = new AtomicLong(1);

    @Autowired
    public QuoteTemplateService(CrmQuoteTemplateMapper templateMapper,
                               CrmQuoteTemplateProcessMapper processMapper,
                               DocNoGenerator docNoGenerator) {
        this.templateMapper = templateMapper;
        this.processMapper = processMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * 创建报价范本
     */
    @Transactional
    public Result<CrmQuoteTemplate> createTemplate(CrmQuoteTemplate template,
                                                   List<CrmQuoteTemplateProcess> processes,
                                                   Long operatorUserId) {
        // 1. 生成范本编号
        String templateNo = generateTemplateNo();
        template.setTemplateNo(templateNo);
        template.setCreatedBy(operatorUserId);
        template.setIsActive(1);

        // 2. 计算利润率默认值
        if (template.getProfitMargin() == null) {
            template.setProfitMargin(new BigDecimal("0.20"));
        }

        // 3. 保存范本
        templateMapper.insert(template);

        // 4. 保存工序明细
        if (processes != null && !processes.isEmpty()) {
            int seq = 0;
            for (CrmQuoteTemplateProcess process : processes) {
                process.setTemplateId(template.getId());
                process.setSequence(seq++);
                processMapper.insert(process);
            }
        }

        return Result.ok(template);
    }

    /**
     * 更新报价范本
     */
    @Transactional
    public Result<CrmQuoteTemplate> updateTemplate(Long id, CrmQuoteTemplate template,
                                                   List<CrmQuoteTemplateProcess> processes) {
        CrmQuoteTemplate existing = templateMapper.selectById(id);
        if (existing == null) {
            return Result.fail(40401, "TEMPLATE_NOT_FOUND");
        }

        // 更新范本基本信息
        existing.setTemplateName(template.getTemplateName());
        existing.setCategory(template.getCategory());
        existing.setProcessType(template.getProcessType());
        existing.setCostMaterial(template.getCostMaterial());
        existing.setCostLabor(template.getCostLabor());
        existing.setCostMachine(template.getCostMachine());
        existing.setCostOverhead(template.getCostOverhead());
        existing.setCostOutsource(template.getCostOutsource());
        existing.setProfitMargin(template.getProfitMargin());
        existing.setBillingMethod(template.getBillingMethod());
        existing.setUnit(template.getUnit());
        existing.setRemark(template.getRemark());
        templateMapper.updateById(existing);

        // 更新工序明细：先删后插
        if (processes != null) {
            // 删除旧工序
            processMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CrmQuoteTemplateProcess>()
                    .eq(CrmQuoteTemplateProcess::getTemplateId, id));
            // 插入新工序
            int seq = 0;
            for (CrmQuoteTemplateProcess process : processes) {
                process.setTemplateId(id);
                process.setSequence(seq++);
                processMapper.insert(process);
            }
        }

        return Result.ok(existing);
    }

    /**
     * 根据ID查询范本（含工序明细）
     */
    public Result<Map<String, Object>> getTemplateWithProcesses(Long id) {
        CrmQuoteTemplate template = templateMapper.selectById(id);
        if (template == null) {
            return Result.fail(40401, "TEMPLATE_NOT_FOUND");
        }

        List<CrmQuoteTemplateProcess> processes = processMapper.selectByTemplateId(id);

        Map<String, Object> result = new HashMap<>();
        result.put("template", template);
        result.put("processes", processes);
        return Result.ok(result);
    }

    /**
     * 查询所有启用的范本
     */
    public Result<List<CrmQuoteTemplate>> listActiveTemplates() {
        return Result.ok(templateMapper.selectAllActive());
    }

    /**
     * 根据分类查询范本
     */
    public Result<List<CrmQuoteTemplate>> listByCategory(String category) {
        return Result.ok(templateMapper.selectByCategory(category));
    }

    /**
     * 根据工艺类型查询范本
     */
    public Result<List<CrmQuoteTemplate>> listByProcessType(String processType) {
        return Result.ok(templateMapper.selectByProcessType(processType));
    }

    /**
     * 禁用范本
     */
    @Transactional
    public Result<Void> disableTemplate(Long id) {
        CrmQuoteTemplate template = templateMapper.selectById(id);
        if (template == null) {
            return Result.fail(40401, "TEMPLATE_NOT_FOUND");
        }
        template.setIsActive(0);
        templateMapper.updateById(template);
        return Result.ok();
    }

    /**
     * 根据范本计算报价
     *
     * @param templateId 范本ID
     * @param quantity   数量
     * @param surfaceArea 表处面积（可选）
     * @return 计算后的单价
     */
    public Result<Map<String, Object>> calculatePrice(Long templateId, Integer quantity, BigDecimal surfaceArea) {
        CrmQuoteTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            return Result.fail(40401, "TEMPLATE_NOT_FOUND");
        }

        List<CrmQuoteTemplateProcess> processes = processMapper.selectByTemplateId(templateId);

        // 计算总工时（分钟）
        int totalMinutes = processes.stream()
                .mapToInt(p -> p.getUnitTimeMinutes() != null ? p.getUnitTimeMinutes() : 0)
                .sum();
        BigDecimal totalHours = new BigDecimal(totalMinutes).divide(new BigDecimal(60), 2, RoundingMode.HALF_UP);

        // 计算成本
        BigDecimal materialCost = template.getCostMaterial().multiply(new BigDecimal(quantity));
        BigDecimal laborCost = template.getCostLabor().multiply(totalHours);
        BigDecimal machineCost = template.getCostMachine().multiply(totalHours);
        BigDecimal overheadCost = laborCost.multiply(template.getCostOverhead().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP));
        BigDecimal outsourceCost = template.getCostOutsource();

        BigDecimal totalCost = materialCost.add(laborCost).add(machineCost).add(overheadCost).add(outsourceCost);

        // 计算利润
        BigDecimal profit = totalCost.multiply(template.getProfitMargin());
        BigDecimal totalPrice = totalCost.add(profit);

        // 按计费方式调整
        BigDecimal unitPrice;
        if ("BY_QUANTITY".equals(template.getBillingMethod())) {
            unitPrice = totalPrice.divide(new BigDecimal(quantity), 2, RoundingMode.HALF_UP);
        } else if ("BY_AREA".equals(template.getBillingMethod()) && surfaceArea != null) {
            unitPrice = totalPrice.divide(surfaceArea, 2, RoundingMode.HALF_UP);
        } else {
            unitPrice = totalPrice;  // 兜底
        }

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", templateId);
        result.put("quantity", quantity);
        result.put("totalHours", totalHours);
        result.put("materialCost", materialCost);
        result.put("laborCost", laborCost);
        result.put("machineCost", machineCost);
        result.put("overheadCost", overheadCost);
        result.put("outsourceCost", outsourceCost);
        result.put("totalCost", totalCost);
        result.put("profitMargin", template.getProfitMargin());
        result.put("profit", profit);
        result.put("totalPrice", totalPrice);
        result.put("unitPrice", unitPrice);
        result.put("processCount", processes.size());
        result.put("processSummary", processes);

        return Result.ok(result);
    }

    /**
     * 生成范本编号 MB-YYYYMMDD-NNNN
     */
    private String generateTemplateNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = seqCounter.getAndIncrement();
        return String.format("MB-%s-%04d", date, seq);
    }
}
