package com.btsheng.erp.production.machine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "设备状态变更日志（prod_machine_status_log）")
@TableName("prod_machine_status_log")
public class ProdMachineStatusLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("machine_id") private Long machineId;
    @TableField("from_status") private String fromStatus;
    @TableField("to_status") private String toStatus;
    @TableField("reason") private String reason;
    @TableField("estimated_recovery_date") private String estimatedRecoveryDate;
    @TableField("changed_by") private String changedBy;
    @TableField("changed_at") private LocalDateTime changedAt;
}