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
@Schema(description = "设备机台（prod_machine）")
@TableName("prod_machine")
public class ProdMachine implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("machine_code") private String machineCode;
    @TableField("machine_name") private String machineName;
    @TableField("machine_type") private String machineType;
    @TableField("machine_no") private String machineNo;
    @TableField("status") private String status = "IDLE";
    @TableField("last_maintenance") private LocalDateTime lastMaintenance;
    @TableField("maintenance_cycle_days") private Integer maintenanceCycleDays = 90;
    @TableField("remark") private String remark;
    @TableField("is_active") private Integer isActive = 1;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
