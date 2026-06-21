package com.btsheng.erp.business.crm.warehouse.scan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.warehouse.scan.entity.CrmWarehouseIncomingScan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmWarehouseIncomingScanMapper extends BaseMapper<CrmWarehouseIncomingScan> {

    @Select("SELECT * FROM crm_warehouse_incoming_scan WHERE scan_no = #{scanNo} LIMIT 1")
    CrmWarehouseIncomingScan selectByNo(@Param("scanNo") String scanNo);

    @Select("SELECT * FROM crm_warehouse_incoming_scan " +
            "WHERE (#{userId} IS NULL OR user_id = #{userId}) " +
            "AND (#{status} IS NULL OR scan_status = #{status}) " +
            "ORDER BY scan_time DESC LIMIT #{limit}")
    List<CrmWarehouseIncomingScan> selectList(@Param("userId") Long userId,
                                               @Param("status") String status,
                                               @Param("limit") int limit);
}
