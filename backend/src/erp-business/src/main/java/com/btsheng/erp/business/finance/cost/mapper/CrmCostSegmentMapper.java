package com.btsheng.erp.business.finance.cost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.finance.cost.entity.CrmCostSegment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmCostSegmentMapper extends BaseMapper<CrmCostSegment> {

    @Select("SELECT * FROM crm_cost_segment WHERE cost_id = #{costId} ORDER BY id ASC")
    List<CrmCostSegment> selectByCostId(@Param("costId") Long costId);
}
