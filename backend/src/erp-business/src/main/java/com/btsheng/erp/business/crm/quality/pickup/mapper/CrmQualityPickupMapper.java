package com.btsheng.erp.business.crm.quality.pickup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.quality.pickup.entity.CrmQualityPickup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CrmQualityPickupMapper extends BaseMapper<CrmQualityPickup> {

    @Select("SELECT * FROM crm_quality_pickup WHERE pickup_no = #{pickupNo} LIMIT 1")
    CrmQualityPickup selectByNo(@Param("pickupNo") String pickupNo);

    @Select("SELECT * FROM crm_quality_pickup WHERE scan_no = #{scanNo} LIMIT 1")
    CrmQualityPickup selectByScanNo(@Param("scanNo") String scanNo);
}
