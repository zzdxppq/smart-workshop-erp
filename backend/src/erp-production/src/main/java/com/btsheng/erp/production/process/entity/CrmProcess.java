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
 * V1.3.7 · Story 1.10 · AC-3.4 工艺库主表
 *
 * 5 段成本聚合：原材料 / 粗加工 / 精加工 / 表面处理 / 检验
 * 3 P2 修补：5 段成本自动聚合 / 工艺复用 / 工艺变更历史
 */
@Data
@Schema(description = "工艺库（crm_process）")
@TableName("crm_process")
public class CrmProcess implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("process_code") private String processCode;            // PROC{yyyyMMdd}{seq:4}
            @TableField("process_name") private String processName;
    @TableField("process_type") private String processType = "STANDARD";
    @TableField("description") private String description;
    @TableField("total_steps") private Integer totalSteps = 0;
    @TableField("total_estimated_hours") private BigDecimal totalEstimatedHours = BigDecimal.ZERO;
    @TableField("total_cost") private BigDecimal totalCost = BigDecimal.ZERO;
    @TableField("cost_breakdown") private String costBreakdown;          // 5 段成本明细 JSON
            @TableField("drawing_id") private Long drawingId;
    @TableField("drawing_no") private String drawingNo;
    @TableField("is_reusable") private Integer isReusable = 1;            // P2 修补：工艺复用
            @TableField("is_active") private Integer isActive = 1;
    @TableField("owner_user_id") private Long ownerUserId;
    @TableField("comment") private String comment;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
