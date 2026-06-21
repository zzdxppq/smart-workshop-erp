package com.btsheng.erp.business.crm.warehouselocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmWarehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmWarehouseMapper extends BaseMapper<CrmWarehouse> {
    @Select("SELECT * FROM crm_warehouse WHERE warehouse_code = #{code} LIMIT 1")
    CrmWarehouse selectByCode(@Param("code") String code);

    @Select("SELECT * FROM crm_warehouse WHERE is_active = 1 ORDER BY warehouse_code")
    List<CrmWarehouse> selectAll();
}
