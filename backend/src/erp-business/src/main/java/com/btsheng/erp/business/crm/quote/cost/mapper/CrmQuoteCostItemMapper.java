package com.btsheng.erp.business.crm.quote.cost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.quote.cost.entity.CrmQuoteCostItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQuoteCostItemMapper extends BaseMapper<CrmQuoteCostItem> {

    @Select("SELECT * FROM crm_quote_cost_item WHERE is_active = 1 ORDER BY sort_order, id")
    List<CrmQuoteCostItem> selectAllActive();
}
