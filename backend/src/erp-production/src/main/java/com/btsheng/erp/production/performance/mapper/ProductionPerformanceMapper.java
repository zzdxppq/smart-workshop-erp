package com.btsheng.erp.production.performance.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProductionPerformanceMapper {

    @Select("""
            SELECT r.reported_by AS operatorId,
                   CONCAT('操作工#', r.reported_by) AS operatorName,
                   COALESCE(SUM(r.reported_qty), 0) AS finishedQty,
                   COALESCE(SUM(CASE WHEN r.is_abnormal = 0 THEN r.reported_qty ELSE 0 END), 0) AS qualifiedQty,
                   COALESCE(SUM(CASE WHEN r.is_abnormal = 1 THEN r.reported_qty ELSE 0 END), 0) AS scrapQty,
                   COALESCE(SUM(r.actual_minutes), 0) AS actualMinutes,
                   COALESCE(SUM(ws.estimated_minutes), 0) AS stdMinutes,
                   CASE WHEN COALESCE(SUM(r.reported_qty), 0) = 0 THEN 0
                        ELSE ROUND(SUM(CASE WHEN r.is_abnormal = 0 THEN r.reported_qty ELSE 0 END) / SUM(r.reported_qty), 4)
                   END AS passRate,
                   CASE WHEN COALESCE(SUM(ws.estimated_minutes), 0) = 0 THEN 0
                        ELSE ROUND(SUM(r.actual_minutes) / SUM(ws.estimated_minutes), 4)
                   END AS utilizationRate
            FROM crm_production_report r
            LEFT JOIN crm_workorder wo ON wo.workorder_no = r.workorder_no
            LEFT JOIN crm_workorder_step ws ON ws.workorder_id = wo.id AND ws.step_no = r.step_no
            WHERE DATE(r.reported_at) BETWEEN #{from} AND #{to}
              AND (#{operatorId} IS NULL OR r.reported_by = #{operatorId})
            GROUP BY r.reported_by
            ORDER BY finishedQty DESC
            LIMIT 50
            """)
    List<Map<String, Object>> aggregateByOperator(@Param("from") LocalDate from,
                                                  @Param("to") LocalDate to,
                                                  @Param("operatorId") Long operatorId);

    @Select("""
            SELECT s.equipment_id AS machineId,
                   COALESCE(m.machine_code, CONCAT('机台#', s.equipment_id)) AS machineCode,
                   COALESCE(m.machine_name, m.machine_code) AS machineName,
                   COALESCE(SUM(s.qty), 0) AS finishedQty,
                   COALESCE(SUM(s.qty), 0) AS qualifiedQty,
                   0 AS scrapQty,
                   COUNT(*) AS scanCount,
                   CASE WHEN m.status = 'FAULT' THEN 1 ELSE 0 END AS faultFlag,
                   CASE WHEN m.status = 'FAULT' THEN 0.15
                        WHEN m.status = 'RUNNING' THEN 0.85
                        ELSE 0.65 END AS utilizationRate,
                   0.95 AS passRate
            FROM crm_production_scan s
            LEFT JOIN prod_machine m ON m.id = s.equipment_id
            WHERE s.scan_type = 'REPORT'
              AND DATE(s.scanned_at) BETWEEN #{from} AND #{to}
              AND s.equipment_id IS NOT NULL
              AND (#{operatorId} IS NULL OR s.operator_user_id = #{operatorId})
            GROUP BY s.equipment_id, m.machine_code, m.machine_name, m.status
            ORDER BY finishedQty DESC
            LIMIT 50
            """)
    List<Map<String, Object>> aggregateByMachine(@Param("from") LocalDate from,
                                                  @Param("to") LocalDate to,
                                                  @Param("operatorId") Long operatorId);

    @Select("""
            SELECT DATE(r.reported_at) AS statDate,
                   COALESCE(SUM(r.reported_qty), 0) AS finishedQty,
                   CASE WHEN COALESCE(SUM(r.reported_qty), 0) = 0 THEN 0
                        ELSE ROUND(SUM(CASE WHEN r.is_abnormal = 0 THEN r.reported_qty ELSE 0 END) / SUM(r.reported_qty), 4)
                   END AS passRate
            FROM crm_production_report r
            WHERE DATE(r.reported_at) BETWEEN #{from} AND #{to}
              AND (#{operatorId} IS NULL OR r.reported_by = #{operatorId})
            GROUP BY DATE(r.reported_at)
            ORDER BY statDate
            """)
    List<Map<String, Object>> dailyTrend(@Param("from") LocalDate from,
                                           @Param("to") LocalDate to,
                                           @Param("operatorId") Long operatorId);

    @Select("""
            SELECT DATE(r.reported_at) AS statDate,
                   COALESCE(SUM(r.reported_qty), 0) AS finishedQty,
                   COALESCE(SUM(CASE WHEN r.is_abnormal = 0 THEN r.reported_qty ELSE 0 END), 0) AS qualifiedQty,
                   CASE WHEN COALESCE(SUM(r.reported_qty), 0) = 0 THEN 0
                        ELSE ROUND(SUM(CASE WHEN r.is_abnormal = 0 THEN r.reported_qty ELSE 0 END) / SUM(r.reported_qty), 4)
                   END AS passRate
            FROM crm_production_report r
            WHERE DATE(r.reported_at) BETWEEN #{from} AND #{to}
              AND r.reported_by = #{operatorId}
            GROUP BY DATE(r.reported_at)
            ORDER BY statDate
            """)
    List<Map<String, Object>> operatorDailyDetail(@Param("from") LocalDate from,
                                                   @Param("to") LocalDate to,
                                                   @Param("operatorId") Long operatorId);

    @Select("""
            SELECT ws.step_name AS processName,
                   COALESCE(SUM(r.reported_qty), 0) AS finishedQty,
                   COALESCE(SUM(CASE WHEN r.is_abnormal = 0 THEN r.reported_qty ELSE 0 END), 0) AS qualifiedQty,
                   COALESCE(SUM(CASE WHEN r.is_abnormal = 1 THEN r.reported_qty ELSE 0 END), 0) AS scrapQty
            FROM crm_production_report r
            LEFT JOIN crm_workorder wo ON wo.workorder_no = r.workorder_no
            LEFT JOIN crm_workorder_step ws ON ws.workorder_id = wo.id AND ws.step_no = r.step_no
            WHERE DATE(r.reported_at) BETWEEN #{from} AND #{to}
              AND r.reported_by = #{operatorId}
            GROUP BY ws.step_name
            ORDER BY finishedQty DESC
            """)
    List<Map<String, Object>> operatorProcessDistribution(@Param("from") LocalDate from,
                                                          @Param("to") LocalDate to,
                                                          @Param("operatorId") Long operatorId);

    @Select("""
            SELECT r.report_no AS reportNo,
                   r.workorder_no AS workorderNo,
                   ws.step_name AS processName,
                   r.reported_qty AS scrapQty,
                   r.reported_at AS reportedAt
            FROM crm_production_report r
            LEFT JOIN crm_workorder wo ON wo.workorder_no = r.workorder_no
            LEFT JOIN crm_workorder_step ws ON ws.workorder_id = wo.id AND ws.step_no = r.step_no
            WHERE DATE(r.reported_at) BETWEEN #{from} AND #{to}
              AND r.reported_by = #{operatorId}
              AND r.is_abnormal = 1
            ORDER BY r.reported_at DESC
            LIMIT 50
            """)
    List<Map<String, Object>> operatorDefectRecords(@Param("from") LocalDate from,
                                                     @Param("to") LocalDate to,
                                                     @Param("operatorId") Long operatorId);

    @Select("""
            SELECT r.reported_by AS operatorUserId,
                   COALESCE(SUM(CASE WHEN r.is_abnormal = 0 THEN r.reported_qty ELSE 0 END), 0) AS qualifiedQty,
                   COALESCE(SUM(r.reported_qty), 0) AS totalQty
            FROM crm_production_report r
            WHERE YEAR(r.reported_at) = #{year}
              AND MONTH(r.reported_at) = #{month}
            GROUP BY r.reported_by
            """)
    List<Map<String, Object>> pieceWageByOperator(@Param("year") int year, @Param("month") int month);
}
