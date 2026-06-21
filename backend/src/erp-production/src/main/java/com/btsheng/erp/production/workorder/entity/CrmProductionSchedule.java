package com.btsheng.erp.production.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "排产（crm_production_schedule）")
@TableName("crm_production_schedule")
public class CrmProductionSchedule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("schedule_no")     private String scheduleNo;
    @TableField("workorder_id")    private Long workorderId;
    @TableField("equipment_id")    private Long equipmentId;
    @TableField("equipment_type")  private String equipmentType;
    @TableField("plan_start")      private LocalDateTime planStart;
    @TableField("plan_end")        private LocalDateTime planEnd;
    @TableField("status")          private String status = "PLANNED";
    @TableField("conflict_with")   private Long conflictWith;
    @TableField("created_at")      private LocalDateTime createdAt;
}
