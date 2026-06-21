package com.btsheng.erp.business.crm.dashboard.multidim.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.dashboard.multidim.entity.CrmDashboardSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmDashboardSnapshotMapper extends BaseMapper<CrmDashboardSnapshot> {

    @Select("SELECT * FROM crm_dashboard_snapshot " +
            "WHERE dimension = #{dimension} " +
            "AND (#{dept} IS NULL OR dim_dept = #{dept}) " +
            "AND (#{category} IS NULL OR dim_category = #{category}) " +
            "AND (#{period} IS NULL OR dim_period = #{period}) " +
            "ORDER BY id ASC")
    List<CrmDashboardSnapshot> selectByDimension(@Param("dimension") String dimension,
                                                 @Param("dept") String dept,
                                                 @Param("category") String category,
                                                 @Param("period") String period);

    @Select("SELECT dim_period AS period, metric_name AS name, metric_value AS value, metric_unit AS unit " +
            "FROM crm_dashboard_snapshot WHERE dimension = #{dimension} " +
            "ORDER BY dim_period ASC")
    List<Map<String, Object>> selectTrend(@Param("dimension") String dimension);
}
