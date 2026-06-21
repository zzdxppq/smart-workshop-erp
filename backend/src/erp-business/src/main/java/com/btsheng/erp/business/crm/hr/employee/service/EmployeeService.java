package com.btsheng.erp.business.crm.hr.employee.service;

import com.btsheng.erp.business.crm.hr.attendance.entity.CrmHrAttendance;
import com.btsheng.erp.business.crm.hr.attendance.mapper.CrmHrAttendanceMapper;
import com.btsheng.erp.business.crm.hr.employee.dto.EmployeeRequest;
import com.btsheng.erp.business.crm.hr.employee.entity.CrmHrEmployee;
import com.btsheng.erp.business.crm.hr.employee.mapper.CrmHrEmployeeMapper;
import com.btsheng.erp.business.crm.hr.employee.util.LoginUsernameGenerator;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.integration.client.PlatformUserClient;
import com.btsheng.erp.business.platform.mapper.PlatformLookupMapper;
import com.btsheng.erp.core.integration.IntegrationEventPublisher;
import com.btsheng.erp.core.integration.IntegrationStreams;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.41 · 员工档案 Service (FR-10-1)
 *
 * 4 业务方法：createEmployee / getEmployee / updateEmployee / listEmployees
 * 工号 EM{yyyyMM}{seq:4}（按月隔离）
 * 3 P1 修补：工号唯一 / 考勤时间冲突 / 跳过请假状态（1.2 SkipOnLeaveRule 复用�? */
@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_LEAVE = "ON_LEAVE";
    public static final String STATUS_RESIGNED = "RESIGNED";

    private final CrmHrEmployeeMapper employeeMapper;
    private final CrmHrAttendanceMapper attendanceMapper;
    private final DocNoGenerator docNoGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;
    private final PlatformUserClient platformUserClient;
    private final PlatformLookupMapper platformLookupMapper;
    private final HrEmployeeResolver employeeResolver;

    @Autowired
    public EmployeeService(CrmHrEmployeeMapper employeeMapper,
                           CrmHrAttendanceMapper attendanceMapper,
                           DocNoGenerator docNoGenerator,
                           IntegrationEventPublisher integrationEventPublisher,
                           PlatformUserClient platformUserClient,
                           PlatformLookupMapper platformLookupMapper,
                           HrEmployeeResolver employeeResolver) {
        this.employeeMapper = employeeMapper;
        this.attendanceMapper = attendanceMapper;
        this.docNoGenerator = docNoGenerator;
        this.integrationEventPublisher = integrationEventPublisher;
        this.platformUserClient = platformUserClient;
        this.platformLookupMapper = platformLookupMapper;
        this.employeeResolver = employeeResolver;
    }

    /** V94 · 员工岗位字典类型 */
    public static final String DICT_EMPLOYEE_POSITION = "EMPLOYEE_POSITION";

    /** V94 · 校验 position 必须是 EMPLOYEE_POSITION 字典值（POS-*），null/空允许（保留旧文本） */
    private Result<Void> validatePosition(String position) {
        if (position == null || position.isBlank()) {
            return Result.ok();
        }
        // 已存在的非 POS-* 文本（如旧种子 "CNC 操作员"/"质检员"）允许保留，不强制迁移
        if (!position.startsWith("POS-")) {
            return Result.ok();
        }
        int exists = platformLookupMapper.existsDictCode(DICT_EMPLOYEE_POSITION, position);
        if (exists == 0) {
            return Result.fail(42200, "INVALID_POSITION: " + position);
        }
        return Result.ok();
    }

    /**
     * AC-10.1.1 创建员工档案
     * P1 修补 1：工号唯一（DB 唯一索引兜底 + 服务层校验）
     */
    @Transactional
    @AuditLog(module = "hr.employee", action = "employee.create")
    public Result<CrmHrEmployee> createEmployee(EmployeeRequest req, Long operatorUserId) {
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            return Result.fail(40001, "EMPLOYEE_NAME_REQUIRED");
        }
        // V94 · 岗位字典校验（POS-* 编码必须属于 EMPLOYEE_POSITION）
        Result<Void> posCheck = validatePosition(req.getPosition());
        if (!posCheck.isSuccess()) {
            return Result.fail(posCheck.getCode(), posCheck.getMessage());
        }
        if (req.getDepartment() == null || req.getDepartment().isEmpty()) {
            if (req.getDeptId() == null) {
                return Result.fail(40001, "DEPARTMENT_REQUIRED");
            }
            String deptName = platformLookupMapper.findDeptNameById(req.getDeptId());
            if (deptName == null || deptName.isBlank()) {
                return Result.fail(40401, "DEPT_NOT_FOUND");
            }
            req.setDepartment(deptName);
        } else if (req.getDeptId() != null) {
            String deptName = platformLookupMapper.findDeptNameById(req.getDeptId());
            if (deptName != null && !deptName.isBlank()) {
                req.setDepartment(deptName);
            }
        }
        CrmHrEmployee e = new CrmHrEmployee();
        String employeeNo = resolveEmployeeNo(req.getEmployeeNo());
        Result<Void> noCheck = validateEmployeeNoUnique(employeeNo, null);
        if (!noCheck.isSuccess()) {
            return Result.fail(noCheck.getCode(), noCheck.getMessage());
        }
        e.setEmployeeNo(employeeNo);
        e.setName(req.getName().trim());
        e.setDepartment(req.getDepartment());
        e.setPosition(req.getPosition());
        e.setPhone(req.getPhone());
        e.setEmail(req.getEmail());
        e.setHireDate(req.getHireDate());
        e.setStatus(req.getStatus() == null ? STATUS_ACTIVE : req.getStatus());
        e.setOnLeave(0);
        e.setBaseSalary(req.getBaseSalary() == null ? java.math.BigDecimal.ZERO : req.getBaseSalary());
        e.setSalaryPackageId(req.getSalaryPackageId());
        e.setPerformanceSchemeId(req.getPerformanceSchemeId());
        e.setReviewerUserId(req.getReviewerUserId());
        e.setUserId(req.getUserId());
        e.setCreatedBy(operatorUserId);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());

        if (e.getUserId() == null && shouldCreateLogin(req)) {
            Result<Long> userResult = provisionLoginAccount(e, req);
            if (!userResult.isSuccess()) {
                return Result.fail(userResult.getCode(), userResult.getMessage());
            }
            e.setUserId(userResult.getData());
        }

        try {
            employeeMapper.insert(e);
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            return Result.fail(40901, "EMPLOYEE_NO_DUPLICATE");
        }
        syncUserAvailability(e, null);
        return Result.ok(e);
    }

    private boolean shouldCreateLogin(EmployeeRequest req) {
        return req.getCreateLoginAccount() == null || Boolean.TRUE.equals(req.getCreateLoginAccount());
    }

    /** PRD AC-10.1.1 · 建档时自动关联 sys_user 登录账号 */
    private Result<Long> provisionLoginAccount(CrmHrEmployee draft, EmployeeRequest req) {
        String username = normalizeLoginUsername(req.getLoginUsername(), draft.getEmployeeNo(), draft.getName());
        if (username == null || username.isBlank()) {
            return Result.fail(40001, "LOGIN_USERNAME_INVALID");
        }
        Long existing = platformLookupMapper.findUserIdByUsername(username);
        if (existing != null) {
            return Result.ok(existing);
        }
        String password = req.getLoginPassword();
        if (password == null || password.isBlank()) {
            password = username;
        }
        String roleCode = req.getRoleCode();
        if (roleCode == null || roleCode.isBlank()) {
            roleCode = inferRoleCode(draft.getPosition());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("hrAutoProvision", true);
        body.put("realName", draft.getName());
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            body.put("phone", req.getPhone());
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            body.put("email", req.getEmail());
        }
        Long deptId = req.getDeptId();
        if (deptId == null && draft.getDepartment() != null) {
            deptId = platformLookupMapper.findDeptIdByName(draft.getDepartment());
        }
        if (deptId != null) {
            body.put("deptId", deptId);
        }
        body.put("roleCodes", List.of(roleCode));
        try {
            Result<Map<String, Object>> created = platformUserClient.createUser(body);
            if (created == null || !created.isSuccess() || created.getData() == null) {
                String msg = created != null ? created.getMessage() : "USER_CREATE_FAILED";
                return Result.fail(50001, msg != null ? msg : "USER_CREATE_FAILED");
            }
            Object idObj = created.getData().get("id");
            if (idObj instanceof Number n) {
                return Result.ok(n.longValue());
            }
            return Result.fail(50001, "USER_CREATE_RESPONSE_INVALID");
        } catch (Exception ex) {
            log.warn("[EmployeeService] provision login failed username={}: {}", username, ex.getMessage());
            return Result.fail(50001, "USER_SERVICE_UNAVAILABLE");
        }
    }

    private String resolveEmployeeNo(String requested) {
        if (requested == null || requested.isBlank()) {
            return docNoGenerator.nextEmployeeNo();
        }
        return requested.trim();
    }

    private Result<Void> validateEmployeeNoUnique(String employeeNo, Long excludeId) {
        if (employeeNo == null || employeeNo.isBlank()) {
            return Result.fail(40001, "EMPLOYEE_NO_REQUIRED");
        }
        CrmHrEmployee exist = employeeMapper.selectByEmployeeNo(employeeNo);
        if (exist != null && (excludeId == null || !exist.getId().equals(excludeId))) {
            return Result.fail(40901, "EMPLOYEE_NO_DUPLICATE");
        }
        return Result.ok(null);
    }

    private String normalizeLoginUsername(String loginUsername, String employeeNo, String name) {
        if (loginUsername != null && !loginUsername.isBlank()) {
            return loginUsername.trim().toLowerCase();
        }
        return LoginUsernameGenerator.generate(name, employeeNo,
                u -> platformLookupMapper.findUserIdByUsername(u) != null);
    }

    private String inferRoleCode(String position) {
        if (position == null) {
            return "OPERATOR";
        }
        String p = position.toLowerCase();
        if (p.contains("仓管") || p.contains("叉车")) {
            return "WAREHOUSE";
        }
        if (p.contains("采购")) {
            return "BUYER";
        }
        if (p.contains("质检") || p.contains("iqc") || p.contains("品检")) {
            return "QC";
        }
        if (p.contains("销售") || p.contains("业务")) {
            return "SALES";
        }
        if (p.contains("财务") || p.contains("会计") || p.contains("出纳")) {
            return "FINANCE";
        }
        if (p.contains("hr") || p.contains("人事") || p.contains("薪酬")) {
            return "HR";
        }
        if (p.contains("工程师") || p.contains("编程")) {
            return "ENGINEER";
        }
        if (p.contains("生管") || p.contains("计划")) {
            return "PROD_MGR";
        }
        return "OPERATOR";
    }

    /**
     * AC-10.1.1 档案详情
     */
    @Transactional(readOnly = true)
    public Result<CrmHrEmployee> getEmployee(Long id) {
        if (id == null) return Result.fail(40001, "EMPLOYEE_ID_REQUIRED");
        CrmHrEmployee e = employeeMapper.selectById(id);
        if (e == null) return Result.fail(40401, "EMPLOYEE_NOT_FOUND");
        return Result.ok(e);
    }

    @Transactional(readOnly = true)
    public Result<CrmHrEmployee> getEmployeeByUserId(Long userId) {
        if (userId == null) return Result.fail(40001, "USER_ID_REQUIRED");
        CrmHrEmployee e = employeeResolver.resolve(userId);
        if (e == null) return Result.fail(40401, "EMPLOYEE_NOT_FOUND");
        return Result.ok(e);
    }

    /**
     * AC-10.1.1 档案更新
     */
    @Transactional
    @AuditLog(module = "hr.employee", action = "employee.update")
    public Result<CrmHrEmployee> updateEmployee(Long id, EmployeeRequest req) {
        CrmHrEmployee exist = employeeMapper.selectById(id);
        if (exist == null) return Result.fail(40401, "EMPLOYEE_NOT_FOUND");
        // V94 · 岗位字典校验
        if (req.getPosition() != null) {
            Result<Void> posCheck = validatePosition(req.getPosition());
            if (!posCheck.isSuccess()) {
                return Result.fail(posCheck.getCode(), posCheck.getMessage());
            }
        }
        if (req.getName() != null) exist.setName(req.getName().trim());
        if (req.getEmployeeNo() != null && !req.getEmployeeNo().isBlank()) {
            String newNo = req.getEmployeeNo().trim();
            Result<Void> noCheck = validateEmployeeNoUnique(newNo, id);
            if (!noCheck.isSuccess()) {
                return Result.fail(noCheck.getCode(), noCheck.getMessage());
            }
            exist.setEmployeeNo(newNo);
        }
        if (req.getDepartment() != null) exist.setDepartment(req.getDepartment());
        if (req.getPosition() != null) exist.setPosition(req.getPosition());
        if (req.getPhone() != null) exist.setPhone(req.getPhone());
        if (req.getEmail() != null) exist.setEmail(req.getEmail());
        if (req.getHireDate() != null) exist.setHireDate(req.getHireDate());
        if (req.getStatus() != null) exist.setStatus(req.getStatus());
        if (req.getBaseSalary() != null) exist.setBaseSalary(req.getBaseSalary());
        if (req.getSalaryPackageId() != null) exist.setSalaryPackageId(req.getSalaryPackageId());
        if (req.getPerformanceSchemeId() != null) exist.setPerformanceSchemeId(req.getPerformanceSchemeId());
        if (req.getReviewerUserId() != null) exist.setReviewerUserId(req.getReviewerUserId());
        if (req.getUserId() != null) exist.setUserId(req.getUserId());
        exist.setUpdatedAt(LocalDateTime.now());
        employeeMapper.updateById(exist);
        syncUserAvailability(exist, null);
        return Result.ok(exist);
    }

    /**
     * AC-10.1.1 分页查询
     * V94 · 新增 position 筛选（POS-* 字典编码）
     */
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> listEmployees(String department, String status, String position, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 200) size = 20;
        int offset = (page - 1) * size;
        List<Map<String, Object>> rows = employeeMapper.selectEmployees(department, status, position, size, offset);
        for (Map<String, Object> row : rows) {
            normalizeEmployeeRowKeys(row);
            Object uid = row.get("userId");
            if (uid instanceof Number n && n.longValue() > 0) {
                String login = platformLookupMapper.findUsernameByUserId(n.longValue());
                if (login != null) {
                    row.put("loginUsername", login);
                }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("page", page);
        data.put("size", size);
        return Result.ok(data);
    }

    /**
     * P1 修补 2/3：考勤时间冲突检�?+ 跳过请假状态（复用 1.2 SkipOnLeaveRule�?     * 返回 true=应跳过（不计入有效出勤），false=正常打卡
     */
    public boolean shouldSkipClock(CrmHrEmployee employee) {
        if (employee == null) return true;
        // 复用 1.2 SkipOnLeaveRule：员�?on_leave=1 时打卡无�?
            return employee.getOnLeave() != null && employee.getOnLeave() == 1;
    }

    /**
     * P1 修补 2：考勤时间冲突（同员工同方�?5 分钟内重复打�?�?拒绝 409�?     */
    public boolean hasClockConflict(Long employeeId, String clockType, LocalDateTime clockAt) {
        CrmHrAttendance recent = attendanceMapper.selectRecentSameType(
                employeeId, clockType, clockAt.minusMinutes(5), clockAt.plusSeconds(1));
        return recent != null;
    }

    /** 将 HR 员工状态同步到 platform.sys_user，供审批流 SkipOnLeaveRule 本地查询 */
    private void syncUserAvailability(CrmHrEmployee employee, String leaveNo) {
        if (employee == null || employee.getUserId() == null) {
            return;
        }
        try {
            String availability = mapAvailabilityStatus(employee);
            Map<String, String> payload = new HashMap<>();
            payload.put("userId", String.valueOf(employee.getUserId()));
            payload.put("availabilityStatus", availability);
            if (leaveNo != null) {
                payload.put("leaveNo", leaveNo);
            }
            integrationEventPublisher.publish(IntegrationStreams.EVENT_USER_AVAILABILITY_CHANGED, payload);
        } catch (Exception ex) {
            log.warn("[EmployeeService] sync user availability failed userId={}: {}",
                    employee.getUserId(), ex.getMessage());
        }
    }

    private String mapAvailabilityStatus(CrmHrEmployee employee) {
        if (employee.getOnLeave() != null && employee.getOnLeave() == 1) {
            return STATUS_LEAVE;
        }
        String status = employee.getStatus();
        if (status == null || status.isBlank()) {
            return "ON_DUTY";
        }
        return switch (status) {
            case STATUS_LEAVE, "ON_TRIP", STATUS_RESIGNED, "DISABLED" -> status;
            default -> "ON_DUTY";
        };
    }

    /** MyBatis Map 查询结果为 snake_case，补全前端常用的 camelCase 字段 */
    private static void normalizeEmployeeRowKeys(Map<String, Object> row) {
        putCamelAlias(row, "employeeNo", "employee_no");
        putCamelAlias(row, "hireDate", "hire_date");
        putCamelAlias(row, "userId", "user_id");
        putCamelAlias(row, "createdAt", "created_at");
        putCamelAlias(row, "updatedAt", "updated_at");
        putCamelAlias(row, "baseSalary", "base_salary");
        putCamelAlias(row, "salaryPackageId", "salary_package_id");
        putCamelAlias(row, "performanceSchemeId", "performance_scheme_id");
        putCamelAlias(row, "reviewerUserId", "reviewer_user_id");
        putCamelAlias(row, "createdBy", "created_by");
        putCamelAlias(row, "onLeave", "on_leave");
    }

    private static void putCamelAlias(Map<String, Object> row, String camelKey, String snakeKey) {
        if (row.containsKey(camelKey) || !row.containsKey(snakeKey)) {
            return;
        }
        row.put(camelKey, row.get(snakeKey));
    }
}
