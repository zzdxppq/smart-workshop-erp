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
@Schema(description = "设备维护记录（prod_machine_maintenance）")
@TableName("prod_machine_maintenance")
public class ProdMachineMaintenance implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("machine_id") private Long machineId;
    @TableField("maintenance_type") private String maintenanceType;
    @TableField("performed_at") private LocalDateTime performedAt;
    @TableField("next_due") private LocalDateTime nextDue;
    @TableField("executor") private String executor;
    @TableField("remark") private String remark;
    @TableField("created_at") private LocalDateTime createdAt;
}
