package com.btsheng.erp.business.crm.quality.pickup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.quality.pickup.entity.CrmQualityPickupItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityPickupItemMapper extends BaseMapper<CrmQualityPickupItem> {

    @Select("SELECT * FROM crm_quality_pickup_item WHERE pickup_no = #{pickupNo} ORDER BY pickup_item_no ASC")
    List<CrmQualityPickupItem> selectByPickupNo(@Param("pickupNo") String pickupNo);
}
