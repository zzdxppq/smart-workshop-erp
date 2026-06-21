package com.btsheng.erp.business.crm.planner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.planner.entity.CrmProcessAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V2.1 · 工序分配 Mapper
 */
@Mapper
public interface CrmProcessAssignmentMapper extends BaseMapper<CrmProcessAssignment> {

    @Select("SELECT * FROM crm_process_assignment WHERE planning_id = #{planningId} ORDER BY sequence")
    List<CrmProcessAssignment> selectByPlanningId(@Param("planningId") Long planningId);
}
