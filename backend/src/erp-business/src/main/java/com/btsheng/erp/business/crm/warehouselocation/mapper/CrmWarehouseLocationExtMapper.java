package com.btsheng.erp.business.crm.warehouselocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmWarehouseLocationExt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmWarehouseLocationExtMapper extends BaseMapper<CrmWarehouseLocationExt> {

    @Select("SELECT * FROM crm_warehouse_location WHERE location_code = #{code} LIMIT 1")
    CrmWarehouseLocationExt selectByLocationCode(@Param("code") String code);

    @Select("SELECT * FROM crm_warehouse_location WHERE warehouse = #{warehouse} AND is_active = 1 ORDER BY location_code")
    List<CrmWarehouseLocationExt> selectByWarehouse(@Param("warehouse") String warehouse);

    @Select("SELECT warehouse, COUNT(*) AS location_count, SUM(capacity) AS total_capacity " +
            "FROM crm_warehouse_location WHERE is_active = 1 GROUP BY warehouse")
    List<Map<String, Object>> aggregateByWarehouse();

    @Select("SELECT * FROM crm_warehouse_location WHERE is_active = 1 ORDER BY warehouse, zone, position")
    List<CrmWarehouseLocationExt> selectAll();
}
