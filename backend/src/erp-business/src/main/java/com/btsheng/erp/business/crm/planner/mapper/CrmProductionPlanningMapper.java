package com.btsheng.erp.business.crm.planner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.planner.entity.CrmProductionPlanning;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V2.1 · 排产计划 Mapper
 */
@Mapper
public interface CrmProductionPlanningMapper extends BaseMapper<CrmProductionPlanning> {

    @Select("SELECT * FROM crm_production_planning WHERE order_id = #{orderId} LIMIT 1")
    CrmProductionPlanning selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM crm_production_planning WHERE status = #{status} ORDER BY created_at DESC")
    List<CrmProductionPlanning> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM crm_production_planning WHERE planner_user_id = #{userId} ORDER BY created_at DESC")
    List<CrmProductionPlanning> selectByPlanner(@Param("userId") Long userId);

    @Select("SELECT * FROM crm_production_planning WHERE status IN ('PENDING', 'ASSIGNED', 'SCHEDULED') ORDER BY planned_start ASC")
    List<CrmProductionPlanning> selectPending();
}
