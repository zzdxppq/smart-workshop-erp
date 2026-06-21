package com.btsheng.erp.business.crm.hr.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "创建/更新员工档案请求")
public class EmployeeRequest {
    @Schema(description = "员工姓名") private String name;
    @Schema(description = "工号（可选，留空则系统自动生成）") private String employeeNo;
    @Schema(description = "部门")     private String department;
    @Schema(description = "部门 ID（与 sys_dept 对齐，优先于 department 文本）") private Long deptId;
    @Schema(description = "岗位")     private String position;
    @Schema(description = "电话")     private String phone;
    @Schema(description = "邮箱")     private String email;
    @Schema(description = "入职日期")  private LocalDate hireDate;
    @Schema(description = "在职状态")  private String status;
    @Schema(description = "基础工资")  private BigDecimal baseSalary;
    @Schema(description = "工资账套ID") private Long salaryPackageId;
    @Schema(description = "考核方案ID") private Long performanceSchemeId;
    @Schema(description = "考核人用户ID") private Long reviewerUserId;
    @Schema(description = "关联系统用户ID（可选；createLoginAccount=true 时自动创建）") private Long userId;
    @Schema(description = "是否同步创建登录账号（PRD AC-10.1.1，默认 true）") private Boolean createLoginAccount;
    @Schema(description = "登录名（可选，默认姓名全拼+随机2位数字，或 bts+工号）") private String loginUsername;
    @Schema(description = "初始密码（可选，默认与登录名相同）") private String loginPassword;
    @Schema(description = "系统角色编码（默认 OPERATOR）") private String roleCode;
}
