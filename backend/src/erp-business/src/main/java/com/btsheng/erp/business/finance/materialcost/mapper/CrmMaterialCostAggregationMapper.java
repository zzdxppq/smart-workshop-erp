package com.btsheng.erp.business.finance.materialcost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.finance.materialcost.entity.CrmMaterialCostAggregation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmMaterialCostAggregationMapper extends BaseMapper<CrmMaterialCostAggregation> {

    @Select("SELECT * FROM crm_material_cost_aggregation WHERE material_code = #{materialCode} " +
            "ORDER BY agg_month DESC, vendor_id ASC")
    List<CrmMaterialCostAggregation> selectByMaterial(@Param("materialCode") String materialCode);

    @Select("SELECT * FROM crm_material_cost_aggregation " +
            "ORDER BY material_code ASC, agg_month DESC, vendor_id ASC")
    List<CrmMaterialCostAggregation> selectAll();

    @Select("SELECT material_code, material_name, " +
            "agg_month, " +
            "SUM(material_amount)     AS material_amount, " +
            "SUM(process_amount)      AS process_amount, " +
            "SUM(outsource_amount)    AS outsource_amount, " +
            "SUM(manage_amount)       AS manage_amount, " +
            "SUM(depreciation_amount) AS depreciation_amount, " +
            "SUM(total_cost)          AS total_cost, " +
            "SUM(qty)                 AS qty " +
            "FROM crm_material_cost_aggregation " +
            "GROUP BY material_code, material_name, agg_month " +
            "ORDER BY material_code ASC, agg_month ASC")
    List<Map<String, Object>> selectCostTrend();

    @Select("SELECT vendor_id, vendor_name, " +
            "SUM(material_amount)     AS material_amount, " +
            "SUM(process_amount)      AS process_amount, " +
            "SUM(outsource_amount)    AS outsource_amount, " +
            "SUM(manage_amount)       AS manage_amount, " +
            "SUM(depreciation_amount) AS depreciation_amount, " +
            "SUM(total_cost)          AS total_cost, " +
            "AVG(unit_cost)           AS avg_unit_cost, " +
            "SUM(qty)                 AS total_qty " +
            "FROM crm_material_cost_aggregation " +
            "WHERE material_code = #{materialCode} " +
            "GROUP BY vendor_id, vendor_name " +
            "ORDER BY total_cost ASC")
    List<Map<String, Object>> selectVendorComparison(@Param("materialCode") String materialCode);
}
