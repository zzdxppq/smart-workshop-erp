package com.btsheng.erp.business.crm.hr.employee.service;

import com.btsheng.erp.business.crm.hr.employee.entity.CrmHrEmployee;
import com.btsheng.erp.business.crm.hr.employee.mapper.CrmHrEmployeeMapper;
import com.btsheng.erp.business.platform.mapper.PlatformLookupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 按 sys_user.id 解析 HR 员工档案；演示账号自动绑定 employee_no。
 */
@Service
public class HrEmployeeResolver {

    private static final Map<String, String> DEMO_USERNAME_EMPLOYEE_NO = Map.of(
            "operator", "EM20260101-0002",
            "qc", "EM20260101-0003",
            "warehouse", "EM20260101-0008",
            "prod_mgr", "EM20260101-0001",
            "buyer", "EM20260101-0005",
            "engineer", "EM20260101-0004",
            "procurement_manager", "EM20260101-0005"
    );

    private final CrmHrEmployeeMapper employeeMapper;
    private final PlatformLookupMapper platformLookup;

    @Autowired
    public HrEmployeeResolver(CrmHrEmployeeMapper employeeMapper, PlatformLookupMapper platformLookup) {
        this.employeeMapper = employeeMapper;
        this.platformLookup = platformLookup;
    }

    @Transactional
    public CrmHrEmployee resolve(Long userId) {
        if (userId == null) {
            return null;
        }
        CrmHrEmployee emp = employeeMapper.selectByUserId(userId);
        if (emp != null) {
            return emp;
        }
        String username = platformLookup.findUsernameByUserId(userId);
        if (username == null || username.isBlank()) {
            return null;
        }
        String employeeNo = DEMO_USERNAME_EMPLOYEE_NO.get(username.trim().toLowerCase());
        if (employeeNo == null) {
            return null;
        }
        emp = employeeMapper.selectByEmployeeNo(employeeNo);
        if (emp == null) {
            return null;
        }
        if (emp.getUserId() == null || !emp.getUserId().equals(userId)) {
            emp.setUserId(userId);
            emp.setUpdatedAt(LocalDateTime.now());
            employeeMapper.updateById(emp);
        }
        return emp;
    }
}
