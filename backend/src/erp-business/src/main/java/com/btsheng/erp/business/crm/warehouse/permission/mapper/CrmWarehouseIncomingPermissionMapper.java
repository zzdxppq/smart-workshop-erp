package com.btsheng.erp.business.crm.warehouse.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.warehouse.permission.entity.CrmWarehouseIncomingPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CrmWarehouseIncomingPermissionMapper extends BaseMapper<CrmWarehouseIncomingPermission> {

    @Select("SELECT * FROM crm_warehouse_incoming_permission " +
            "WHERE permission_no = #{permissionNo} LIMIT 1")
    CrmWarehouseIncomingPermission selectByNo(@Param("permissionNo") String permissionNo);

    @Select("SELECT * FROM crm_warehouse_incoming_permission " +
            "WHERE user_id = #{userId} AND status = 'ACTIVE' " +
            "AND valid_to >= NOW() ORDER BY valid_to DESC LIMIT 1")
    CrmWarehouseIncomingPermission selectActiveByUser(@Param("userId") Long userId);
}
