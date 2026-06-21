package com.btsheng.erp.business.crm.hr.scheme.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("crm_hr_salary_package")
public class CrmHrSalaryPackage implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("package_name") private String packageName;
    @TableField("position") private String position;
    @TableField("base_salary") private BigDecimal baseSalary;
    @TableField("position_salary") private BigDecimal positionSalary;
    @TableField("piece_unit_price") private BigDecimal pieceUnitPrice;
    @TableField("performance_a_rate") private BigDecimal performanceARate;
    @TableField("performance_b_rate") private BigDecimal performanceBRate;
    @TableField("performance_c_rate") private BigDecimal performanceCRate;
    @TableField("performance_d_rate") private BigDecimal performanceDRate;
    @TableField("overtime_rate") private BigDecimal overtimeRate;
    @TableField("full_attendance_bonus") private BigDecimal fullAttendanceBonus;
    @TableField("social_insurance_rate") private BigDecimal socialInsuranceRate;
    @TableField("tax_threshold") private BigDecimal taxThreshold;
    @TableField("tax_rate") private BigDecimal taxRate;
    @TableField("is_default") private Integer isDefault;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
