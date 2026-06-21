package com.btsheng.erp.business.crm.hr.payroll.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "薪酬单")
@TableName("crm_hr_payroll")
public class CrmHrPayroll implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("payroll_no")     private String payrollNo;
    @TableField("period_year")    private Integer periodYear;
    @TableField("period_month")   private Integer periodMonth;
    @TableField("employee_id")    private Long employeeId;
    @TableField("employee_no")    private String employeeNo;
    @TableField("employee_name")  private String employeeName;
    @TableField("base_salary")    private BigDecimal baseSalary = BigDecimal.ZERO;
    @TableField("position_salary") private BigDecimal positionSalary = BigDecimal.ZERO;
    @TableField("piece_pay")      private BigDecimal piecePay = BigDecimal.ZERO;
    @TableField("performance_bonus") private BigDecimal performanceBonus = BigDecimal.ZERO;
    @TableField("overtime_hours") private BigDecimal overtimeHours = BigDecimal.ZERO;
    @TableField("overtime_pay")   private BigDecimal overtimePay = BigDecimal.ZERO;
    @TableField("bonus")          private BigDecimal bonus = BigDecimal.ZERO;
    @TableField("full_attendance_bonus") private BigDecimal fullAttendanceBonus = BigDecimal.ZERO;
    @TableField("deduction")      private BigDecimal deduction = BigDecimal.ZERO;
    @TableField("social_insurance") private BigDecimal socialInsurance = BigDecimal.ZERO;
    @TableField("tax")            private BigDecimal tax = BigDecimal.ZERO;
    @TableField("net_salary")     private BigDecimal netSalary = BigDecimal.ZERO;
    @TableField("status")         private String status = "DRAFT";
    @TableField("approved_by")    private Long approvedBy;
    @TableField("approved_at")    private LocalDateTime approvedAt;
    @TableField("created_by")     private Long createdBy;
    @TableField("created_at")     private LocalDateTime createdAt;
    @TableField("updated_at")     private LocalDateTime updatedAt;
}
