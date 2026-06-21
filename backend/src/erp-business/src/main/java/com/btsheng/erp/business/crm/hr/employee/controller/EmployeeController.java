package com.btsheng.erp.business.crm.hr.employee.controller;

import com.btsheng.erp.business.crm.hr.employee.dto.EmployeeRequest;
import com.btsheng.erp.business.crm.hr.employee.entity.CrmHrEmployee;
import com.btsheng.erp.business.crm.hr.employee.service.EmployeeService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * V1.3.7 · Story 1.41 · 员工档案 Controller (E10-HR-Employee)
 * 4 端点
 */
@Tag(name = "E10-HR-Employee", description = "人事·员工档案")
@RestController
@RequestMapping("/hr/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Operation(summary = "创建员工档案")
    @PostMapping
    public Result<CrmHrEmployee> create(@RequestBody EmployeeRequest req,
                                        @RequestParam(required = false) Long operatorUserId) {
        return employeeService.createEmployee(req, operatorUserId == null ? 1L : operatorUserId);
    }

    @Operation(summary = "员工档案详情")
    @GetMapping("/{id}")
    public Result<CrmHrEmployee> get(@PathVariable Long id) {
        return employeeService.getEmployee(id);
    }

    @Operation(summary = "更新员工档案")
    @PutMapping("/{id}")
    public Result<CrmHrEmployee> update(@PathVariable Long id, @RequestBody EmployeeRequest req) {
        return employeeService.updateEmployee(id, req);
    }

    @Operation(summary = "员工档案分页查询")
    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) String department,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String position,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return employeeService.listEmployees(department, status, position, page, size);
    }

    @Operation(summary = "当前登录用户对应员工档案")
    @GetMapping("/me")
    public Result<CrmHrEmployee> me(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return employeeService.getEmployeeByUserId(userId == null ? 1L : userId);
    }
}
