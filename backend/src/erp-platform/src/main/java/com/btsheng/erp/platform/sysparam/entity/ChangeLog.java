package com.btsheng.erp.platform.sysparam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** V1.3.7 Story 1.3 · sys_change_log · V1.3.7 红线 5（变更走变更日志） */
@Data
@Schema(description = "变更日志（sys_change_log）")
@TableName("sys_change_log")
public class ChangeLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("entity")
    @Schema(description = "实体：threshold/dict/param", example = "threshold")
    private String entity;

    @TableField("entity_id")
    @Schema(description = "业务实体 ID", example = "2")
    private Long entityId;

    @TableField("operation")
    @Schema(description = "操作：CREATE/UPDATE/DELETE", example = "UPDATE")
    private String operation;

    @TableField("before_value")
    @Schema(description = "改前值（JSON）", example = "{\"threshold\":200000.00}")
    private String beforeValue;

    @TableField("after_value")
    @Schema(description = "改后值（JSON）", example = "{\"threshold\":250000.00}")
    private String afterValue;

    @TableField("changed_by")
    private Long changedBy;

    @TableField("changed_at")
    private LocalDateTime changedAt;
}
