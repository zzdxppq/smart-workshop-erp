package com.btsheng.erp.business.crm.pricecontrol.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.pricecontrol.entity.CrmPriceControl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmPriceControlMapper extends BaseMapper<CrmPriceControl> {

    @Select("SELECT * FROM crm_price_control WHERE material_id = #{materialId} AND vendor_id IS NULL ORDER BY effective_date DESC LIMIT 1")
    CrmPriceControl selectGenericByMaterial(@Param("materialId") Long materialId);

    @Select("SELECT * FROM crm_price_control WHERE material_id = #{materialId} AND vendor_id = #{vendorId} ORDER BY effective_date DESC LIMIT 1")
    CrmPriceControl selectByMaterialAndVendor(@Param("materialId") Long materialId, @Param("vendorId") Long vendorId);

    @Select("SELECT * FROM crm_price_control WHERE material_id = #{materialId} AND status = 'ACTIVE' ORDER BY effective_date DESC")
    List<CrmPriceControl> selectActiveByMaterial(@Param("materialId") Long materialId);

    @Select("SELECT * FROM crm_price_control WHERE status = 'ACTIVE' ORDER BY effective_date DESC")
    List<CrmPriceControl> selectActiveAll();
}
