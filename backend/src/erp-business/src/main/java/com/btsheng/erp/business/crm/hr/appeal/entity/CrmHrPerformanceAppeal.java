package com.btsheng.erp.business.crm.hr.appeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("crm_hr_performance_appeal")
public class CrmHrPerformanceAppeal implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("performance_id") private Long performanceId;
    @TableField("employee_id") private Long employeeId;
    @TableField("employee_name") private String employeeName;
    @TableField("period_year") private Integer periodYear;
    @TableField("period_month") private Integer periodMonth;
    @TableField("reason") private String reason;
    @TableField("status") private String status = "PENDING";
    @TableField("reviewer_user_id") private Long reviewerUserId;
    @TableField("reply") private String reply;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("resolved_at") private LocalDateTime resolvedAt;
}
