package com.btsheng.erp.business.crm.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.41 · 考勤记录（crm_hr_attendance）
 */
@Data
@Schema(description = "考勤记录")
@TableName("crm_hr_attendance")
public class CrmHrAttendance implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("employee_id")   private Long employeeId;
    @TableField("employee_no")   private String employeeNo;
    @TableField("clock_type")    private String clockType;
    @TableField("clock_at")      private LocalDateTime clockAt;
    @TableField("is_on_leave")   private Integer isOnLeave = 0;
    @TableField("effective")     private Integer effective = 1;
    @TableField("remark")        private String remark;
    @TableField("created_at")    private LocalDateTime createdAt;
}
