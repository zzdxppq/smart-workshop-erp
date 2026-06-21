package com.btsheng.erp.business.crm.engineer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.engineer.entity.CrmEngineeringWorkbench;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V2.1 · 工程转化工作台 Mapper
 */
@Mapper
public interface CrmEngineeringWorkbenchMapper extends BaseMapper<CrmEngineeringWorkbench> {

    @Select("SELECT * FROM crm_engineering_workbench WHERE order_id = #{orderId} ORDER BY id")
    List<CrmEngineeringWorkbench> selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM crm_engineering_workbench WHERE order_id = #{orderId} AND order_item_id = #{orderItemId} LIMIT 1")
    CrmEngineeringWorkbench selectByOrderItemId(@Param("orderId") Long orderId, @Param("orderItemId") Long orderItemId);

    @Select("SELECT * FROM crm_engineering_workbench WHERE status = #{status} ORDER BY created_at ASC")
    List<CrmEngineeringWorkbench> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM crm_engineering_workbench WHERE engineer_user_id = #{userId} AND status IN ('PENDING', 'IN_PROGRESS') ORDER BY created_at ASC")
    List<CrmEngineeringWorkbench> selectPendingByEngineer(@Param("userId") Long userId);

    @Select("SELECT * FROM crm_engineering_workbench WHERE status = 'PENDING' ORDER BY created_at ASC")
    List<CrmEngineeringWorkbench> selectAllPending();
}
