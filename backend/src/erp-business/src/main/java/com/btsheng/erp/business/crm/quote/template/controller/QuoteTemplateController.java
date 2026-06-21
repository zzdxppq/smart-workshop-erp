package com.btsheng.erp.business.crm.quote.template.controller;

import com.btsheng.erp.business.crm.quote.template.entity.CrmQuoteTemplate;
import com.btsheng.erp.business.crm.quote.template.entity.CrmQuoteTemplateProcess;
import com.btsheng.erp.business.crm.quote.template.service.QuoteTemplateService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * V2.1 · 报价范本 Controller
 */
@Tag(name = "E2-QuoteTemplate", description = "报价范本管理（V2.1）")
@RestController
@RequestMapping("/quote-templates")
public class QuoteTemplateController {

    private final QuoteTemplateService templateService;

    @Autowired
    public QuoteTemplateController(QuoteTemplateService templateService) {
        this.templateService = templateService;
    }

    @Operation(summary = "创建报价范本")
    @PostMapping
    public Result<CrmQuoteTemplate> create(@RequestBody Map<String, Object> request) {
        CrmQuoteTemplate template = parseTemplate(request);
        List<CrmQuoteTemplateProcess> processes = parseProcesses(request);
        return templateService.createTemplate(template, processes, 1L);
    }

    @Operation(summary = "更新报价范本")
    @PutMapping("/{id}")
    public Result<CrmQuoteTemplate> update(@PathVariable("id") Long id, @RequestBody Map<String, Object> request) {
        CrmQuoteTemplate template = parseTemplate(request);
        List<CrmQuoteTemplateProcess> processes = parseProcesses(request);
        return templateService.updateTemplate(id, template, processes);
    }

    @Operation(summary = "查询范本详情（含工序）")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable("id") Long id) {
        return templateService.getTemplateWithProcesses(id);
    }

    @Operation(summary = "查询所有启用的范本")
    @GetMapping
    public Result<List<CrmQuoteTemplate>> list() {
        return templateService.listActiveTemplates();
    }

    @Operation(summary = "根据分类查询范本")
    @GetMapping("/category/{category}")
    public Result<List<CrmQuoteTemplate>> listByCategory(@PathVariable("category") String category) {
        return templateService.listByCategory(category);
    }

    @Operation(summary = "根据工艺类型查询范本")
    @GetMapping("/process-type/{processType}")
    public Result<List<CrmQuoteTemplate>> listByProcessType(@PathVariable("processType") String processType) {
        return templateService.listByProcessType(processType);
    }

    @Operation(summary = "禁用范本")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable("id") Long id) {
        return templateService.disableTemplate(id);
    }

    @Operation(summary = "根据范本计算报价")
    @GetMapping("/{id}/calculate")
    public Result<Map<String, Object>> calculate(@PathVariable("id") Long id,
                                                 @RequestParam("quantity") Integer quantity,
                                                 @RequestParam(value = "surfaceArea", required = false) BigDecimal surfaceArea) {
        return templateService.calculatePrice(id, quantity, surfaceArea);
    }

    @SuppressWarnings("unchecked")
    private CrmQuoteTemplate parseTemplate(Map<String, Object> request) {
        CrmQuoteTemplate template = new CrmQuoteTemplate();
        template.setTemplateName((String) request.get("templateName"));
        template.setCategory((String) request.get("category"));
        template.setProcessType((String) request.get("processType"));
        if (request.get("costMaterial") != null) {
            template.setCostMaterial(new BigDecimal(request.get("costMaterial").toString()));
        }
        if (request.get("costLabor") != null) {
            template.setCostLabor(new BigDecimal(request.get("costLabor").toString()));
        }
        if (request.get("costMachine") != null) {
            template.setCostMachine(new BigDecimal(request.get("costMachine").toString()));
        }
        if (request.get("costOverhead") != null) {
            template.setCostOverhead(new BigDecimal(request.get("costOverhead").toString()));
        }
        if (request.get("costOutsource") != null) {
            template.setCostOutsource(new BigDecimal(request.get("costOutsource").toString()));
        }
        if (request.get("profitMargin") != null) {
            template.setProfitMargin(new BigDecimal(request.get("profitMargin").toString()));
        }
        template.setBillingMethod((String) request.getOrDefault("billingMethod", "BY_QUANTITY"));
        template.setUnit((String) request.getOrDefault("unit", "件"));
        template.setRemark((String) request.get("remark"));
        return template;
    }

    @SuppressWarnings("unchecked")
    private List<CrmQuoteTemplateProcess> parseProcesses(Map<String, Object> request) {
        List<Map<String, Object>> processList = (List<Map<String, Object>>) request.get("processes");
        if (processList == null || processList.isEmpty()) {
            return null;
        }
        return processList.stream().map(p -> {
            CrmQuoteTemplateProcess process = new CrmQuoteTemplateProcess();
            process.setProcessCode((String) p.get("processCode"));
            process.setProcessName((String) p.get("processName"));
            process.setMachineType((String) p.get("machineType"));
            if (p.get("unitTimeMinutes") != null) {
                process.setUnitTimeMinutes(Integer.parseInt(p.get("unitTimeMinutes").toString()));
            }
            if (p.get("costPerHour") != null) {
                process.setCostPerHour(new BigDecimal(p.get("costPerHour").toString()));
            }
            process.setOutsourceFlag((Integer) p.getOrDefault("outsourceFlag", 0));
            process.setRemark((String) p.get("remark"));
            return process;
        }).toList();
    }
}
