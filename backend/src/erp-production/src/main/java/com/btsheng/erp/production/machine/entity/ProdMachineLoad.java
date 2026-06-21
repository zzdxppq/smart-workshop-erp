package com.btsheng.erp.production.machine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "机台日负荷（prod_machine_load）")
@TableName("prod_machine_load")
public class ProdMachineLoad implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("machine_id") private Long machineId;
    @TableField("load_date") private LocalDate loadDate;
    @TableField("planned_hours") private BigDecimal plannedHours = BigDecimal.ZERO;
    @TableField("available_hours") private BigDecimal availableHours = new BigDecimal("12");
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
