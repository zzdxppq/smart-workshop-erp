package com.btsheng.erp.business.crm.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.hr.attendance.entity.CrmHrAttendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CrmHrAttendanceMapper extends BaseMapper<CrmHrAttendance> {

    @Select("SELECT * FROM crm_hr_attendance WHERE employee_id = #{employeeId} " +
            "AND clock_type = #{clockType} AND clock_at >= #{from} AND clock_at < #{to} " +
            "ORDER BY clock_at DESC LIMIT 1")
    CrmHrAttendance selectRecentSameType(@Param("employeeId") Long employeeId,
                                         @Param("clockType") String clockType,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    @Select("SELECT * FROM crm_hr_attendance WHERE employee_id = #{employeeId} " +
            "AND clock_at >= #{from} AND clock_at < #{to} ORDER BY clock_at ASC")
    List<CrmHrAttendance> selectByEmployeeAndRange(@Param("employeeId") Long employeeId,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);

    @Select("SELECT clock_type AS clockType, COUNT(*) AS cnt " +
            "FROM crm_hr_attendance WHERE employee_id = #{employeeId} " +
            "AND clock_at >= #{from} AND clock_at < #{to} " +
            "AND effective = 1 GROUP BY clock_type")
    List<Map<String, Object>> countByType(@Param("employeeId") Long employeeId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    @Select("SELECT DATE(a.clock_at) AS date, " +
            "e.name AS employeeName, " +
            "MIN(CASE WHEN a.clock_type = 'IN' THEN TIME(a.clock_at) END) AS checkIn, " +
            "MAX(CASE WHEN a.clock_type = 'OUT' THEN TIME(a.clock_at) END) AS checkOut, " +
            "ROUND(TIMESTAMPDIFF(MINUTE, " +
            "  MIN(CASE WHEN a.clock_type = 'IN' THEN a.clock_at END), " +
            "  MAX(CASE WHEN a.clock_type = 'OUT' THEN a.clock_at END)) / 60.0, 1) AS hours, " +
            "CASE " +
            "  WHEN MIN(CASE WHEN a.clock_type = 'IN' THEN TIME(a.clock_at) END) IS NULL THEN 'ABSENT' " +
            "  WHEN MIN(CASE WHEN a.clock_type = 'IN' THEN TIME(a.clock_at) END) > '08:30:00' THEN 'LATE' " +
            "  ELSE 'NORMAL' END AS status " +
            "FROM crm_hr_attendance a " +
            "LEFT JOIN crm_hr_employee e ON a.employee_id = e.id " +
            "WHERE a.clock_at >= #{from} AND a.clock_at < #{to} AND a.effective = 1 " +
            "GROUP BY DATE(a.clock_at), a.employee_id, e.name " +
            "ORDER BY date DESC, employeeName ASC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectDailyReportByPeriod(@Param("from") LocalDateTime from,
                                                        @Param("to") LocalDateTime to,
                                                        @Param("limit") int limit,
                                                        @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM (" +
            "SELECT 1 FROM crm_hr_attendance a " +
            "WHERE a.clock_at >= #{from} AND a.clock_at < #{to} AND a.effective = 1 " +
            "GROUP BY DATE(a.clock_at), a.employee_id" +
            ") t")
    long countDailyReportByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT a.*, e.name AS employeeName, e.employee_no AS employeeNo " +
            "FROM crm_hr_attendance a " +
            "LEFT JOIN crm_hr_employee e ON a.employee_id = e.id " +
            "WHERE a.clock_at >= #{from} AND a.clock_at < #{to} " +
            "ORDER BY a.clock_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectByPeriod(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM crm_hr_attendance " +
            "WHERE clock_at >= #{from} AND clock_at < #{to}")
    long countByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(DISTINCT DATE(clock_at)) FROM crm_hr_attendance " +
            "WHERE employee_id = #{employeeId} AND clock_type = 'IN' AND effective = 1 " +
            "AND YEAR(clock_at) = #{year} AND MONTH(clock_at) = #{month}")
    int countPresentDays(@Param("employeeId") Long employeeId,
                         @Param("year") int year,
                         @Param("month") int month);

    @Select("SELECT COUNT(*) FROM (" +
            "SELECT DATE(clock_at) d FROM crm_hr_attendance " +
            "WHERE employee_id = #{employeeId} AND clock_type = 'IN' AND effective = 1 " +
            "AND YEAR(clock_at) = #{year} AND MONTH(clock_at) = #{month} " +
            "GROUP BY DATE(clock_at) HAVING MIN(TIME(clock_at)) > '08:30:00'" +
            ") t")
    int countLateDays(@Param("employeeId") Long employeeId,
                      @Param("year") int year,
                      @Param("month") int month);
}
