package com.btsheng.erp.business.crm.planner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.planner.entity.CrmProductionNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V2.1 · 工单通知 Mapper
 */
@Mapper
public interface CrmProductionNotificationMapper extends BaseMapper<CrmProductionNotification> {

    @Select("SELECT * FROM crm_production_notification WHERE planning_id = #{planningId} ORDER BY created_at DESC")
    List<CrmProductionNotification> selectByPlanningId(@Param("planningId") Long planningId);

    @Select("SELECT * FROM crm_production_notification WHERE recipient_user_id = #{userId} AND status = 'PENDING' ORDER BY created_at DESC")
    List<CrmProductionNotification> selectPendingByUser(@Param("userId") Long userId);
}
