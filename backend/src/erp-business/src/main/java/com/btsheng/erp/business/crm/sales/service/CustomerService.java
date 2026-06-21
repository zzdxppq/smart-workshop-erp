package com.btsheng.erp.business.crm.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.sales.entity.CrmCustomer;
import com.btsheng.erp.business.crm.sales.mapper.CrmCustomerMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.SalesDataScopeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerService {

    private final CrmCustomerMapper customerMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public CustomerService(CrmCustomerMapper customerMapper, DocNoGenerator docNoGenerator) {
        this.customerMapper = customerMapper;
        this.docNoGenerator = docNoGenerator;
    }

    public Result<Map<String, Object>> list(Long ownerId, String status, int pageNum, int pageSize) {
        LambdaQueryWrapper<CrmCustomer> qw = new LambdaQueryWrapper<>();
        SalesDataScopeHelper.Scope scope = SalesDataScopeHelper.effectiveScope();
        if (scope == SalesDataScopeHelper.Scope.SELF) {
            Long scopedOwner = SalesDataScopeHelper.resolveOwnerUserId(ownerId);
            if (scopedOwner != null) {
                qw.eq(CrmCustomer::getOwnerId, scopedOwner);
            }
        } else if (scope == SalesDataScopeHelper.Scope.DEPT) {
            Long deptId = SalesDataScopeHelper.resolveDeptId(null);
            if (deptId != null) {
                qw.inSql(CrmCustomer::getId,
                        "SELECT DISTINCT customer_id FROM crm_quote WHERE dept_id = " + deptId
                                + " UNION SELECT DISTINCT customer_id FROM crm_order WHERE dept_id = " + deptId);
            }
        } else if (ownerId != null) {
            qw.eq(CrmCustomer::getOwnerId, ownerId);
        }
        if (status != null && !status.isBlank()) {
            qw.eq(CrmCustomer::getStatus, status.trim());
        }
        qw.orderByDesc(CrmCustomer::getId);
        List<CrmCustomer> all = customerMapper.selectList(qw);

        int size = pageSize > 0 ? pageSize : 20;
        int page = Math.max(pageNum, 1);
        int from = (page - 1) * size;
        int to = Math.min(from + size, all.size());
        List<Map<String, Object>> items = new ArrayList<>();
        if (from < all.size()) {
            for (CrmCustomer c : all.subList(from, to)) {
                items.add(toVo(c));
            }
        }
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("items", items);
        pageData.put("records", items);
        pageData.put("total", all.size());
        pageData.put("pageNum", page);
        pageData.put("pageSize", size);
        return Result.ok(pageData);
    }

    public Result<Map<String, Object>> create(CrmCustomer customer) {
        if (customer.getCustomerCode() == null || customer.getCustomerCode().isBlank()) {
            customer.setCustomerCode(docNoGenerator.nextCustomerCode());
        }
        if (customer.getName() == null || customer.getName().isBlank()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "name 必填");
        }
        if (customer.getOwnerId() == null) {
            customer.setOwnerId(SalesDataScopeHelper.requireOperatorUserId(1L));
        }
        if (customer.getStatus() == null || customer.getStatus().isBlank()) {
            customer.setStatus("ACTIVE");
        }
        customerMapper.insert(customer);
        return Result.ok(toVo(customer));
    }

    public Result<Map<String, Object>> getById(Long id) {
        if (id == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "id 必填");
        }
        CrmCustomer c = customerMapper.selectById(id);
        if (c == null) {
            return Result.fail(40404, "CUSTOMER_NOT_FOUND");
        }
        Result<Void> scope = SalesDataScopeHelper.assertCustomerOwner(c.getOwnerId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        return Result.ok(toVo(c));
    }

    public Result<Map<String, Object>> update(Long id, CrmCustomer patch) {
        if (id == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "id 必填");
        }
        CrmCustomer c = customerMapper.selectById(id);
        if (c == null) {
            return Result.fail(40404, "CUSTOMER_NOT_FOUND");
        }
        Result<Void> scope = SalesDataScopeHelper.assertCustomerOwner(c.getOwnerId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        if (patch.getName() != null && !patch.getName().isBlank()) {
            c.setName(patch.getName().trim());
        }
        if (patch.getIndustry() != null) {
            c.setIndustry(patch.getIndustry());
        }
        if (patch.getCreditLimit() != null) {
            c.setCreditLimit(patch.getCreditLimit());
        }
        if (patch.getStatus() != null && !patch.getStatus().isBlank()) {
            c.setStatus(patch.getStatus().trim());
        }
        if (patch.getContactName() != null) {
            c.setContactName(patch.getContactName().trim());
        }
        if (patch.getContactPhone() != null) {
            c.setContactPhone(patch.getContactPhone().trim());
        }
        if (patch.getContactEmail() != null) {
            c.setContactEmail(patch.getContactEmail().trim());
        }
        customerMapper.updateById(c);
        return Result.ok(toVo(c));
    }

    public Result<Map<String, Object>> claim(Long id, Long operatorUserId) {
        if (id == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "id 必填");
        }
        CrmCustomer c = customerMapper.selectById(id);
        if (c == null) {
            return Result.fail(40404, "CUSTOMER_NOT_FOUND");
        }
        Result<Void> scope = SalesDataScopeHelper.assertCustomerOwner(c.getOwnerId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        long owner = SalesDataScopeHelper.requireOperatorUserId(operatorUserId != null ? operatorUserId : 1L);
        c.setOwnerId(owner);
        c.setProtectUntil(LocalDate.now().plusDays(30));
        customerMapper.updateById(c);
        return Result.ok(toVo(c));
    }

    private Map<String, Object> toVo(CrmCustomer c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("customerCode", c.getCustomerCode());
        m.put("name", c.getName());
        m.put("customerName", c.getName());
        m.put("contactName", c.getContactName());
        m.put("contactPhone", c.getContactPhone());
        m.put("contactEmail", c.getContactEmail());
        m.put("industry", c.getIndustry());
        m.put("creditLimit", c.getCreditLimit());
        m.put("ownerId", c.getOwnerId());
        m.put("protectUntil", c.getProtectUntil());
        m.put("status", c.getStatus());
        return m;
    }
}
