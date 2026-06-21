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
@Schema(description = "工单工序（crm_workorder_step）")
@TableName("crm_workorder_step")
public class CrmWorkorderStep implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("workorder_id")       private Long workorderId;
    @TableField("step_no")            private Integer stepNo;
    @TableField("step_name")          private String stepName;
    @TableField("equipment_type")     private String equipmentType;
    @TableField("machine_id")         private Long machineId;
    @TableField("locked_machine_id")  private Long lockedMachineId;
    @TableField("is_outsource")       private Integer isOutsource = 0;
    @TableField("estimated_minutes")  private Integer estimatedMinutes = 0;
    @TableField("actual_minutes")     private Integer actualMinutes = 0;
    @TableField("status")             private String status = "PENDING";
    @TableField("started_at")         private LocalDateTime startedAt;
    @TableField("completed_at")       private LocalDateTime completedAt;
    @TableField("operator_user_id")   private Long operatorUserId;
}
