package com.btsheng.erp.business.crm.hr.attendance.controller;

import com.btsheng.erp.business.crm.hr.attendance.dto.ClockRequest;
import com.btsheng.erp.business.crm.hr.attendance.entity.CrmHrAttendance;
import com.btsheng.erp.business.crm.hr.attendance.service.AttendanceService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * V1.3.7 · Story 1.41 · 考勤 Controller (E10-HR-Attendance)
 * 1 端点
 */
@Tag(name = "E10-HR-Attendance", description = "人事·考勤打卡")
@RestController
@RequestMapping("/hr/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Autowired
    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Operation(summary = "考勤打卡（IN/OUT/LUNCH_IN/LUNCH_OUT）")
    @PostMapping("/clock")
    public Result<CrmHrAttendance> clock(@RequestBody ClockRequest req) {
        return attendanceService.clockInOut(req);
    }

    @Operation(summary = "考勤打卡（Web 路径别名 /punch）")
    @PostMapping("/punch")
    public Result<CrmHrAttendance> punch(@RequestBody ClockRequest req) {
        return attendanceService.clockInOut(req);
    }

    @Operation(summary = "考勤月报列表（Web 路径）")
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return attendanceService.listAttendances(period, pageNum, pageSize);
    }

    @Operation(summary = "我的考勤记录")
    @GetMapping("/my")
    public Result<Map<String, Object>> my(@RequestParam Long employeeId,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return attendanceService.getMyAttendance(employeeId, from, to);
    }
}
