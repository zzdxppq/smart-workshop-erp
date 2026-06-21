package com.btsheng.erp.business.crm.pricecontrol.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.pricecontrol.entity.CrmPriceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CrmPriceHistoryMapper extends BaseMapper<CrmPriceHistory> {

    @Select("SELECT * FROM crm_price_history WHERE material_id = #{materialId} AND vendor_id = #{vendorId} AND purchased_at >= #{since} ORDER BY purchased_at DESC")
    List<CrmPriceHistory> selectByMaterialVendorSince(@Param("materialId") Long materialId,
                                                      @Param("vendorId") Long vendorId,
                                                      @Param("since") LocalDate since);

    @Select("SELECT * FROM crm_price_history WHERE material_id = #{materialId} AND purchased_at >= #{since} ORDER BY purchased_at DESC")
    List<CrmPriceHistory> selectByMaterialSince(@Param("materialId") Long materialId,
                                                @Param("since") LocalDate since);

    @Select("SELECT AVG(unit_price) FROM crm_price_history WHERE material_id = #{materialId} AND vendor_id = #{vendorId} AND purchased_at >= #{since}")
    BigDecimal avgPrice(@Param("materialId") Long materialId,
                        @Param("vendorId") Long vendorId,
                        @Param("since") LocalDate since);
}
