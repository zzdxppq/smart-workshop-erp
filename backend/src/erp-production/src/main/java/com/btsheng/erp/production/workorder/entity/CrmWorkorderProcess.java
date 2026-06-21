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
@Schema(description = "工单工序（crm_workorder_process · E5-S6）")
@TableName("crm_workorder_process")
public class CrmWorkorderProcess implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("workorder_id") private Long workorderId;
    @TableField("workorder_no") private String workorderNo;
    @TableField("process_seq") private Integer processSeq;
    @TableField("process_code") private String processCode;
    @TableField("process_name") private String processName;
    @TableField("material_code") private String materialCode;
    @TableField("machine_id") private Long machineId;
    @TableField("locked_machine_id") private Long lockedMachineId;
    @TableField("is_outsource") private Integer isOutsource = 0;
    @TableField("status") private String status = "PENDING";
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
