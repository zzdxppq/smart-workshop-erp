package com.btsheng.erp.business.crm.quote.cost.controller;

import com.btsheng.erp.business.crm.quote.cost.entity.CrmQuoteCostItem;
import com.btsheng.erp.business.crm.quote.cost.service.QuoteCostItemService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "E2-Quote-Cost-Item", description = "报价成本项目录（V2.1）")
@RestController
@RequestMapping("/quote-cost-items")
public class QuoteCostItemController {

    private final QuoteCostItemService service;

    @Autowired
    public QuoteCostItemController(QuoteCostItemService service) {
        this.service = service;
    }

    @Operation(summary = "成本项列表")
    @GetMapping
    public Result<List<CrmQuoteCostItem>> list() {
        return service.listAll();
    }

    @Operation(summary = "启用中的成本项")
    @GetMapping("/active")
    public Result<List<CrmQuoteCostItem>> listActive() {
        return service.listActive();
    }

    @Operation(summary = "新增成本项")
    @PostMapping
    public Result<CrmQuoteCostItem> create(@RequestBody CrmQuoteCostItem item) {
        return service.create(item);
    }

    @Operation(summary = "更新成本项")
    @PutMapping("/{id}")
    public Result<CrmQuoteCostItem> update(@PathVariable Long id, @RequestBody CrmQuoteCostItem item) {
        return service.update(id, item);
    }

    @Operation(summary = "删除成本项")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return service.delete(id);
    }
}
