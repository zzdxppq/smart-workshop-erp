package com.btsheng.erp.production.rework.entity;

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

/**
 * V1.3.7 · Story 1.23 · 委外返修单（crm_rework · FR-6-3）
 */
@Data
@Schema(description = "委外返修单（crm_rework · Story 1.23 FR-6-3）")
@TableName("crm_rework")
public class CrmRework implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("rework_no")           private String reworkNo;
    @TableField("outsource_id")        private Long outsourceId;
    @TableField("outsource_no")        private String outsourceNo;
    @TableField("reason")              private String reason;
    @TableField("cost")                private BigDecimal cost = BigDecimal.ZERO;
    @TableField("rework_count")        private Integer reworkCount = 1;
    @TableField("status")              private String status = "DRAFT";
    @TableField("expected_finish_date") private LocalDate expectedFinishDate;
    @TableField("finished_at")         private LocalDateTime finishedAt;
    @TableField("created_by")          private Long createdBy;
    @TableField("created_at")          private LocalDateTime createdAt;
    @TableField("updated_at")          private LocalDateTime updatedAt;
}
