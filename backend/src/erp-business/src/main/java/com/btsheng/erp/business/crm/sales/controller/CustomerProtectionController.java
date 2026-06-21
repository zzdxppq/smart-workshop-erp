package com.btsheng.erp.business.crm.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btsheng.erp.business.crm.sales.entity.CrmCustomer;
import com.btsheng.erp.business.crm.sales.mapper.CrmCustomerMapper;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** E2-S1 客户保护（Web CustomerProtection 页） */
@Tag(name = "E2-Customer-Protection", description = "客户保护管理")
@RestController
@RequestMapping("/customers")
public class CustomerProtectionController {

    private final CrmCustomerMapper customerMapper;

    @Autowired
    public CustomerProtectionController(CrmCustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @Operation(summary = "客户保护列表（保护期内的客户）")
    @GetMapping("/protection")
    public Result<List<Map<String, Object>>> listProtection() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<CrmCustomer> qw = new LambdaQueryWrapper<>();
        qw.isNotNull(CrmCustomer::getOwnerId)
          .ge(CrmCustomer::getProtectUntil, today)
          .orderByDesc(CrmCustomer::getId);
        List<CrmCustomer> customers = customerMapper.selectList(qw);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CrmCustomer c : customers) {
            rows.add(Map.of(
                "id", c.getId(),
                "customerCode", c.getCustomerCode() != null ? c.getCustomerCode() : "",
                "name", c.getName() != null ? c.getName() : "",
                "ownerUserId", c.getOwnerId() != null ? c.getOwnerId() : 0,
                "protectUntil", c.getProtectUntil() != null ? c.getProtectUntil().toString() : "",
                "active", c.getProtectUntil() != null && !c.getProtectUntil().isBefore(today)
            ));
        }
        return Result.ok(rows);
    }
}
