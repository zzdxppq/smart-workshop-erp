package com.btsheng.erp.business.crm.quote.cost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.quote.cost.entity.ProcessCostMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProcessCostMappingMapper extends BaseMapper<ProcessCostMapping> {

    @Select("SELECT * FROM crm_process_cost_mapping WHERE is_active = 1 ORDER BY sort_order, id")
    List<ProcessCostMapping> selectAllActive();
}
