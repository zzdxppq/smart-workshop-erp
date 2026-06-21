package com.btsheng.erp.business.crm.hr.attendance.service;

import com.btsheng.erp.business.crm.hr.attendance.dto.ClockRequest;
import com.btsheng.erp.business.crm.hr.attendance.entity.CrmHrAttendance;
import com.btsheng.erp.business.crm.hr.attendance.mapper.CrmHrAttendanceMapper;
import com.btsheng.erp.business.crm.hr.employee.entity.CrmHrEmployee;
import com.btsheng.erp.business.crm.hr.employee.mapper.CrmHrEmployeeMapper;
import com.btsheng.erp.business.crm.hr.employee.service.EmployeeService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.41 · 考勤打卡 Service
 *
 * 2 业务方法：clockInOut / getMyAttendance
 * 3 P1 修补：工号唯一（依�?employee�? 时间冲突 5min / 跳过请假
 */
@Service
public class AttendanceService {

    public static final String CLOCK_IN = "IN";
    public static final String CLOCK_OUT = "OUT";
    public static final String CLOCK_LUNCH_IN = "LUNCH_IN";
    public static final String CLOCK_LUNCH_OUT = "LUNCH_OUT";

    private final CrmHrAttendanceMapper attendanceMapper;
    private final CrmHrEmployeeMapper employeeMapper;
    private final EmployeeService employeeService;

    @Autowired
    public AttendanceService(CrmHrAttendanceMapper attendanceMapper,
                             CrmHrEmployeeMapper employeeMapper,
                             EmployeeService employeeService) {
        this.attendanceMapper = attendanceMapper;
        this.employeeMapper = employeeMapper;
        this.employeeService = employeeService;
    }

    /**
     * AC-10.1.2 + AC-10.1.3 考勤打卡
     * P1 修补 2：时间冲突（同员工同方向 5 分钟�?�?409�?     * P1 修补 3：跳过请假（on_leave=1 �?effective=0，不计入有效出勤�?     */
    @Transactional
    @AuditLog(module = "hr.attendance", action = "attendance.clock")
    public Result<CrmHrAttendance> clockInOut(ClockRequest req) {
        if (req.getEmployeeId() == null) return Result.fail(40001, "EMPLOYEE_ID_REQUIRED");
        if (req.getClockType() == null) return Result.fail(40001, "CLOCK_TYPE_REQUIRED");
        if (!isValidClockType(req.getClockType())) return Result.fail(40002, "CLOCK_TYPE_INVALID");
        CrmHrEmployee emp = employeeMapper.selectById(req.getEmployeeId());
        if (emp == null) return Result.fail(40401, "EMPLOYEE_NOT_FOUND");

        LocalDateTime clockAt = req.getClockAt() == null ? LocalDateTime.now() : req.getClockAt();

        // P1 修补 2�? 分钟内重复打�?�?409
            if (employeeService.hasClockConflict(req.getEmployeeId(), req.getClockType(), clockAt)) {
            return Result.fail(40901, "CLOCK_CONFLICT");
        }

        CrmHrAttendance a = new CrmHrAttendance();
        a.setEmployeeId(emp.getId());
        a.setEmployeeNo(emp.getEmployeeNo());
        a.setClockType(req.getClockType());
        a.setClockAt(clockAt);
        a.setRemark(req.getRemark());
        // P1 修补 3：复�?1.2 SkipOnLeaveRule
            boolean skip = employeeService.shouldSkipClock(emp);
        a.setIsOnLeave(skip ? 1 : 0);
        a.setEffective(skip ? 0 : 1);
        a.setCreatedAt(LocalDateTime.now());
        attendanceMapper.insert(a);
        return Result.ok(a);
    }

    /**
     * AC-10.1.2 我的考勤（按员工 + 时间范围�?     */
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getMyAttendance(Long employeeId, LocalDateTime from, LocalDateTime to) {
        if (employeeId == null) return Result.fail(40001, "EMPLOYEE_ID_REQUIRED");
        if (from == null) from = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        if (to == null) to = from.plusMonths(1);
        List<CrmHrAttendance> rows = attendanceMapper.selectByEmployeeAndRange(employeeId, from, to);
        List<Map<String, Object>> cnt = attendanceMapper.countByType(employeeId, from, to);
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("summary", cnt);
        data.put("from", from);
        data.put("to", to);
        return Result.ok(data);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> listAttendances(String period, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 200) pageSize = 20;
        YearMonth ym = parsePeriod(period);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = attendanceMapper.selectDailyReportByPeriod(from, to, pageSize, offset);
        long total = attendanceMapper.countDailyReportByPeriod(from, to);
        Map<String, Object> data = new HashMap<>();
        data.put("items", rows);
        data.put("list", rows);
        data.put("records", rows);
        data.put("total", total);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        data.put("period", ym.toString());
        return Result.ok(data);
    }

    private static YearMonth parsePeriod(String period) {
        if (period == null || period.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(period.trim());
        } catch (Exception ex) {
            return YearMonth.now();
        }
    }

    private boolean isValidClockType(String t) {
        return CLOCK_IN.equals(t) || CLOCK_OUT.equals(t)
                || CLOCK_LUNCH_IN.equals(t) || CLOCK_LUNCH_OUT.equals(t);
    }
}
