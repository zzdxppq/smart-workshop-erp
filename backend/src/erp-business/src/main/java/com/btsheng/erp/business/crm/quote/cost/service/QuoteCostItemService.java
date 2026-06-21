package com.btsheng.erp.business.crm.quote.cost.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.business.crm.quote.cost.entity.CrmQuoteCostItem;
import com.btsheng.erp.business.crm.quote.cost.mapper.CrmQuoteCostItemMapper;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuoteCostItemService {

    private final CrmQuoteCostItemMapper mapper;

    @Autowired
    public QuoteCostItemService(CrmQuoteCostItemMapper mapper) {
        this.mapper = mapper;
    }

    public Result<List<CrmQuoteCostItem>> listAll() {
        QueryWrapper<CrmQuoteCostItem> qw = new QueryWrapper<>();
        qw.orderByAsc("sort_order", "id");
        return Result.ok(mapper.selectList(qw));
    }

    public Result<List<CrmQuoteCostItem>> listActive() {
        return Result.ok(mapper.selectAllActive());
    }

    public Result<CrmQuoteCostItem> get(Long id) {
        CrmQuoteCostItem item = mapper.selectById(id);
        if (item == null) {
            return Result.fail(40401, "COST_ITEM_NOT_FOUND");
        }
        return Result.ok(item);
    }

    @Transactional
    public Result<CrmQuoteCostItem> create(CrmQuoteCostItem item) {
        if (item.getItemCode() == null || item.getItemCode().isBlank()) {
            return Result.fail(40001, "ITEM_CODE_REQUIRED");
        }
        QueryWrapper<CrmQuoteCostItem> dup = new QueryWrapper<>();
        dup.eq("item_code", item.getItemCode());
        if (mapper.selectCount(dup) > 0) {
            return Result.fail(40902, "ITEM_CODE_DUPLICATE");
        }
        if (item.getIsActive() == null) {
            item.setIsActive(1);
        }
        mapper.insert(item);
        return Result.ok(item);
    }

    @Transactional
    public Result<CrmQuoteCostItem> update(Long id, CrmQuoteCostItem patch) {
        CrmQuoteCostItem existing = mapper.selectById(id);
        if (existing == null) {
            return Result.fail(40401, "COST_ITEM_NOT_FOUND");
        }
        if (patch.getItemName() != null) existing.setItemName(patch.getItemName());
        if (patch.getBillingMethod() != null) existing.setBillingMethod(patch.getBillingMethod());
        if (patch.getUnit() != null) existing.setUnit(patch.getUnit());
        if (patch.getUnitPrice() != null) existing.setUnitPrice(patch.getUnitPrice());
        if (patch.getProfitMargin() != null) existing.setProfitMargin(patch.getProfitMargin());
        if (patch.getProcessCode() != null) existing.setProcessCode(patch.getProcessCode());
        if (patch.getSortOrder() != null) existing.setSortOrder(patch.getSortOrder());
        if (patch.getIsActive() != null) existing.setIsActive(patch.getIsActive());
        mapper.updateById(existing);
        return Result.ok(existing);
    }

    @Transactional
    public Result<Void> delete(Long id) {
        if (mapper.selectById(id) == null) {
            return Result.fail(40401, "COST_ITEM_NOT_FOUND");
        }
        mapper.deleteById(id);
        return Result.ok();
    }
}
