package com.btsheng.erp.business.crm.warehousescan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.warehousescan.entity.CrmWarehouseLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmWarehouseLocationMapper extends BaseMapper<CrmWarehouseLocation> {

    @Select("SELECT * FROM crm_warehouse_location WHERE location_code = #{code} LIMIT 1")
    CrmWarehouseLocation selectByLocationCode(@Param("code") String code);

    @Select("SELECT * FROM crm_warehouse_location WHERE warehouse = #{warehouse} AND is_active = 1 ORDER BY location_code")
    List<CrmWarehouseLocation> selectByWarehouse(@Param("warehouse") String warehouse);

    @Select("SELECT * FROM crm_warehouse_location WHERE is_active = 1 ORDER BY location_code")
    List<CrmWarehouseLocation> selectAll();
}
