package com.btsheng.erp.business.crm.warehouse.scan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.warehouse.scan.entity.CrmWarehouseIncomingItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmWarehouseIncomingItemMapper extends BaseMapper<CrmWarehouseIncomingItem> {

    @Select("SELECT * FROM crm_warehouse_incoming_item WHERE scan_no = #{scanNo} ORDER BY item_no ASC")
    List<CrmWarehouseIncomingItem> selectByScanNo(@Param("scanNo") String scanNo);

    @Select("SELECT COUNT(*) FROM crm_warehouse_incoming_item WHERE scan_no = #{scanNo}")
    int countByScanNo(@Param("scanNo") String scanNo);
}
