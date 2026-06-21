package com.btsheng.erp.business.crm.hr.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.41 · 员工档案（crm_hr_employee）
 */
@Data
@Schema(description = "员工档案")
@TableName("crm_hr_employee")
public class CrmHrEmployee implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("employee_no")   private String employeeNo;
    @TableField("user_id")       private Long userId;
    @TableField("name")          private String name;
    @TableField("department")    private String department;
    @TableField("position")      private String position;
    @TableField("phone")         private String phone;
    @TableField("email")         private String email;
    @TableField("hire_date")     private LocalDate hireDate;
    @TableField("status")        private String status = "ACTIVE";
    @TableField("on_leave")      private Integer onLeave = 0;
    @TableField("base_salary")   private BigDecimal baseSalary = BigDecimal.ZERO;
    @TableField("salary_package_id") private Long salaryPackageId;
    @TableField("performance_scheme_id") private Long performanceSchemeId;
    @TableField("reviewer_user_id") private Long reviewerUserId;
    @TableField("created_by")    private Long createdBy;
    @TableField("created_at")    private LocalDateTime createdAt;
    @TableField("updated_at")    private LocalDateTime updatedAt;
}
