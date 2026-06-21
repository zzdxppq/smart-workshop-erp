package com.btsheng.erp.production.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.10 · 工序库（5 段 · 严格排序）
 *
 * 3 P1 修补：工序排序严格 / 机器类型匹配 / 工时非负
 */
@Data
@Schema(description = "工序库（crm_process_step）")
@TableName("crm_process_step")
public class CrmProcessStep implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("process_id") private Long processId;
    @TableField("step_no") private Integer stepNo;                       // P1 修补 1：严格排序
            @TableField("step_name") private String stepName;
    @TableField("segment") private String segment = "原材料";            // 5 段
            @TableField("machine_type") private String machineType;              // P1 修补 2：机器类型匹配
    @TableField("machine_id") private Long machineId;
    @TableField("estimated_hours") private BigDecimal estimatedHours = BigDecimal.ZERO;  // P1 修补 3：非负
            @TableField("unit_cost") private BigDecimal unitCost = BigDecimal.ZERO;
    @TableField("description") private String description;
    @TableField("is_quality_check") private Integer isQualityCheck = 0;
    @TableField("created_at") private LocalDateTime createdAt;
}
