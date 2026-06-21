package com.btsheng.erp.business.crm.sales.controller;

import com.btsheng.erp.business.crm.sales.entity.CrmCustomer;
import com.btsheng.erp.business.crm.sales.service.CustomerService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.SalesDataScopeHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "E2-Customers", description = "客户档案")
@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "客户列表")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String status) {
        return customerService.list(ownerId, status, pageNum, pageSize);
    }

    @PostMapping
    @Operation(summary = "创建客户")
    public Result<Map<String, Object>> create(@RequestBody CrmCustomer customer) {
        return customerService.create(customer);
    }

    @GetMapping("/{id}")
    @Operation(summary = "客户详情")
    public Result<Map<String, Object>> getById(@PathVariable("id") Long id) {
        return customerService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新客户档案")
    public Result<Map<String, Object>> update(@PathVariable("id") Long id, @RequestBody CrmCustomer customer) {
        return customerService.update(id, customer);
    }

    @PostMapping("/{id}/claim")
    @Operation(summary = "客户领用（30 天保护期 · E2-S1）")
    public Result<Map<String, Object>> claim(
            @PathVariable("id") Long id) {
        return customerService.claim(id, SalesDataScopeHelper.requireOperatorUserId(1L));
    }
}
