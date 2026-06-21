package com.btsheng.erp.business.crm.hr.appeal.service;

import com.btsheng.erp.business.crm.hr.appeal.dto.PerformanceAppealRequest;
import com.btsheng.erp.business.crm.hr.appeal.dto.PerformanceAppealResolveRequest;
import com.btsheng.erp.business.crm.hr.appeal.entity.CrmHrPerformanceAppeal;
import com.btsheng.erp.business.crm.hr.appeal.mapper.CrmHrPerformanceAppealMapper;
import com.btsheng.erp.business.crm.hr.employee.entity.CrmHrEmployee;
import com.btsheng.erp.business.crm.hr.employee.mapper.CrmHrEmployeeMapper;
import com.btsheng.erp.business.crm.hr.employee.service.HrEmployeeResolver;
import com.btsheng.erp.business.crm.hr.performance.entity.CrmHrPerformance;
import com.btsheng.erp.business.crm.hr.performance.mapper.CrmHrPerformanceMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PerformanceAppealService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final CrmHrPerformanceAppealMapper appealMapper;
    private final CrmHrPerformanceMapper performanceMapper;
    private final CrmHrEmployeeMapper employeeMapper;
    private final HrEmployeeResolver employeeResolver;

    @Autowired
    public PerformanceAppealService(CrmHrPerformanceAppealMapper appealMapper,
                                    CrmHrPerformanceMapper performanceMapper,
                                    CrmHrEmployeeMapper employeeMapper,
                                    HrEmployeeResolver employeeResolver) {
        this.appealMapper = appealMapper;
        this.performanceMapper = performanceMapper;
        this.employeeMapper = employeeMapper;
        this.employeeResolver = employeeResolver;
    }

    @Transactional
    @AuditLog(module = "hr.performance", action = "appeal.submit")
    public Result<CrmHrPerformanceAppeal> submit(PerformanceAppealRequest req, Long userId) {
        if (req.getPerformanceId() == null) return Result.fail(40001, "PERFORMANCE_ID_REQUIRED");
        if (req.getReason() == null || req.getReason().isBlank()) return Result.fail(40001, "REASON_REQUIRED");

        CrmHrPerformance perf = performanceMapper.selectById(req.getPerformanceId());
        if (perf == null) return Result.fail(40401, "PERFORMANCE_NOT_FOUND");

        CrmHrEmployee emp = employeeResolver.resolve(userId);
        if (emp == null || !emp.getId().equals(perf.getEmployeeId())) {
            return Result.fail(40301, "FORBIDDEN");
        }

        CrmHrPerformanceAppeal a = new CrmHrPerformanceAppeal();
        a.setPerformanceId(perf.getId());
        a.setEmployeeId(emp.getId());
        a.setEmployeeName(emp.getName());
        a.setPeriodYear(perf.getPeriodYear());
        a.setPeriodMonth(perf.getPeriodMonth());
        a.setReason(req.getReason().trim());
        a.setStatus(STATUS_PENDING);
        a.setReviewerUserId(emp.getReviewerUserId());
        a.setCreatedAt(LocalDateTime.now());
        appealMapper.insert(a);
        return Result.ok(a);
    }

    @Transactional
    @AuditLog(module = "hr.performance", action = "appeal.resolve")
    public Result<CrmHrPerformanceAppeal> resolve(Long id, PerformanceAppealResolveRequest req, Long operatorUserId) {
        CrmHrPerformanceAppeal a = appealMapper.selectById(id);
        if (a == null) return Result.fail(40401, "APPEAL_NOT_FOUND");
        if (!STATUS_PENDING.equals(a.getStatus())) return Result.fail(40901, "APPEAL_STATUS_INVALID");

        String status = req.getStatus() != null ? req.getStatus().toUpperCase() : STATUS_REJECTED;
        if (!STATUS_APPROVED.equals(status) && !STATUS_REJECTED.equals(status)) {
            return Result.fail(40001, "STATUS_INVALID");
        }
        a.setStatus(status);
        a.setReply(req.getReply());
        a.setReviewerUserId(operatorUserId);
        a.setResolvedAt(LocalDateTime.now());
        appealMapper.updateById(a);
        return Result.ok(a);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> list(Long employeeId, String status, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 200) size = 20;
        int offset = (page - 1) * size;
        List<Map<String, Object>> rows = appealMapper.selectAppeals(employeeId, status, size, offset);
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("page", page);
        data.put("size", size);
        return Result.ok(data);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> myAppeals(Long userId, int page, int size) {
        CrmHrEmployee emp = employeeResolver.resolve(userId);
        if (emp == null) return Result.fail(40401, "EMPLOYEE_NOT_FOUND");
        return list(emp.getId(), null, page, size);
    }
}
