package com.btsheng.erp.business.crm.dashboard.production.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.dashboard.production.entity.CrmDashboardProduction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmDashboardProductionMapper extends BaseMapper<CrmDashboardProduction> {

    @Select("SELECT workorder_status AS status, COUNT(*) AS cnt, AVG(progress) AS avgProgress " +
            "FROM crm_dashboard_production WHERE workorder_no IS NOT NULL " +
            "GROUP BY workorder_status")
    List<Map<String, Object>> selectWorkorderStatusDistribution();

    @Select("SELECT * FROM crm_dashboard_production WHERE workorder_no IS NOT NULL " +
            "ORDER BY snapshot_at DESC LIMIT #{limit}")
    List<CrmDashboardProduction> selectWorkorders(@Param("limit") int limit);

    @Select("SELECT * FROM crm_dashboard_production WHERE alert_type IS NOT NULL " +
            "ORDER BY snapshot_at DESC LIMIT #{limit}")
    List<CrmDashboardProduction> selectAlerts(@Param("limit") int limit);

    @Select("SELECT alert_type AS level, COUNT(*) AS cnt FROM crm_dashboard_production " +
            "WHERE alert_type IS NOT NULL GROUP BY alert_type")
    List<Map<String, Object>> selectAlertLevelDistribution();

    @Select("SELECT COUNT(*) AS totalWorkorders, " +
            "SUM(CASE WHEN workorder_status='RUNNING' THEN 1 ELSE 0 END) AS running, " +
            "SUM(CASE WHEN workorder_status='DONE' THEN 1 ELSE 0 END) AS done, " +
            "SUM(CASE WHEN workorder_status='PENDING' THEN 1 ELSE 0 END) AS pending, " +
            "SUM(CASE WHEN workorder_status='PAUSED' THEN 1 ELSE 0 END) AS paused, " +
            "AVG(progress) AS avgProgress " +
            "FROM crm_dashboard_production WHERE workorder_no IS NOT NULL")
    Map<String, Object> selectOverview();
}
