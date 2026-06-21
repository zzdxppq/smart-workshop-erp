package com.btsheng.erp.business.crm.dashboard.materialprice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.dashboard.materialprice.entity.CrmMaterialPriceDashboard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmMaterialPriceDashboardMapper extends BaseMapper<CrmMaterialPriceDashboard> {

    @Select("SELECT * FROM crm_material_price_dashboard " +
            "WHERE price_type = 'STANDARD' " +
            "AND (#{keyword} IS NULL OR material_code LIKE CONCAT('%', #{keyword}, '%') OR material_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{vendor} IS NULL OR vendor_name = #{vendor}) " +
            "ORDER BY price DESC LIMIT #{limit}")
    List<CrmMaterialPriceDashboard> searchPrice(@Param("keyword") String keyword,
                                                  @Param("vendor") String vendor,
                                                  @Param("limit") int limit);

    @Select("SELECT price_period AS period, AVG(price) AS avgPrice, SUM(cost_total) AS totalCost " +
            "FROM crm_material_price_dashboard " +
            "WHERE price_type = 'TREND' " +
            "AND material_code = #{materialCode} " +
            "AND price_period >= #{from} AND price_period <= #{to} " +
            "GROUP BY price_period ORDER BY price_period ASC")
    List<Map<String, Object>> selectTrend(@Param("materialCode") String materialCode,
                                          @Param("from") String from,
                                          @Param("to") String to);

    @Select("SELECT vendor_name, price, cost_total " +
            "FROM crm_material_price_dashboard " +
            "WHERE price_type = 'VENDOR_COMP' AND material_code = #{materialCode} " +
            "AND price_period = #{period} " +
            "ORDER BY price ASC")
    List<Map<String, Object>> selectVendorCompare(@Param("materialCode") String materialCode,
                                                   @Param("period") String period);
}
