package com.btsheng.erp.business.crm.hr.performance.entity;

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
@Schema(description = "员工绩效")
@TableName("crm_hr_performance")
public class CrmHrPerformance implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("employee_id")    private Long employeeId;
    @TableField("employee_no")    private String employeeNo;
    @TableField("employee_name")  private String employeeName;
    @TableField("period_year")    private Integer periodYear;
    @TableField("period_month")   private Integer periodMonth;
    @TableField("score")          private BigDecimal score;
    @TableField("grade")          private String grade;
    @TableField("kpi_items")      private String kpiItems;
    @TableField("comment")        private String comment;
    @TableField("created_by")     private Long createdBy;
    @TableField("created_at")     private LocalDateTime createdAt;
}
