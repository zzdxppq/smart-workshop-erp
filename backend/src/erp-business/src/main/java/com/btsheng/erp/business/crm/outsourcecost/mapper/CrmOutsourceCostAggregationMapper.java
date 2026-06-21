package com.btsheng.erp.business.crm.outsourcecost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.outsourcecost.entity.CrmOutsourceCostAggregation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmOutsourceCostAggregationMapper extends BaseMapper<CrmOutsourceCostAggregation> {

    @Select("SELECT * FROM crm_outsource_cost_aggregation WHERE outsource_id = #{outsourceId} ORDER BY created_at DESC")
    List<CrmOutsourceCostAggregation> selectByOutsourceId(@Param("outsourceId") Long outsourceId);

    @Select("SELECT * FROM crm_outsource_cost_aggregation WHERE aggregation_scope = #{scope} ORDER BY created_at DESC")
    List<CrmOutsourceCostAggregation> selectByScope(@Param("scope") String scope);

    @Select("SELECT " +
            "  COALESCE(SUM(cost_material), 0) AS sumMaterial, " +
            "  COALESCE(SUM(cost_labor), 0)    AS sumLabor, " +
            "  COALESCE(SUM(cost_machine), 0)  AS sumMachine, " +
            "  COALESCE(SUM(cost_overhead), 0) AS sumOverhead, " +
            "  COALESCE(SUM(cost_outsource),0) AS sumOutsource, " +
            "  COALESCE(SUM(cost_total), 0)    AS sumTotal, " +
            "  COALESCE(SUM(budget_cost), 0)   AS sumBudget " +
            "FROM crm_outsource_cost_aggregation WHERE outsource_id = #{outsourceId}")
    Map<String, Object> selectSegmentSumByOutsourceId(@Param("outsourceId") Long outsourceId);
}
