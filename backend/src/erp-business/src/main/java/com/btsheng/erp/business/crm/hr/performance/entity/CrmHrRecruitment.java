package com.btsheng.erp.business.crm.hr.performance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "招聘记录")
@TableName("crm_hr_recruitment")
public class CrmHrRecruitment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("recruitment_no") private String recruitmentNo;
    @TableField("candidate_name") private String candidateName;
    @TableField("position")       private String position;
    @TableField("department")     private String department;
    @TableField("phone")          private String phone;
    @TableField("email")          private String email;
    @TableField("hr_status")      private String hrStatus = "PENDING";
    @TableField("dept_status")    private String deptStatus = "PENDING";
    @TableField("hrd_status")     private String hrdStatus = "PENDING";
    @TableField("final_status")   private String finalStatus = "RECRUITING";
    @TableField("offer_date")     private LocalDate offerDate;
    @TableField("onboard_date")   private LocalDate onboardDate;
    @TableField("created_by")     private Long createdBy;
    @TableField("created_at")     private LocalDateTime createdAt;
    @TableField("updated_at")     private LocalDateTime updatedAt;
}
