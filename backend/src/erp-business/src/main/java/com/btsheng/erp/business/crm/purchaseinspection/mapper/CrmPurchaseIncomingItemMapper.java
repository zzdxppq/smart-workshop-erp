package com.btsheng.erp.business.crm.purchaseinspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.purchaseinspection.entity.CrmPurchaseIncomingItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmPurchaseIncomingItemMapper extends BaseMapper<CrmPurchaseIncomingItem> {

    @Select("SELECT * FROM crm_purchase_incoming_item WHERE inspection_id = #{inspectionId} ORDER BY seq_no ASC")
    List<CrmPurchaseIncomingItem> selectByInspectionId(@Param("inspectionId") Long inspectionId);
}
