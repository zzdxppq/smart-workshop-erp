package com.btsheng.erp.business.crm.drawing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.7 · 图纸变更历史（@AuditLog AFTER_COMMIT 写入）
 */
@Data
@Schema(description = "图纸变更历史（crm_drawing_history）")
@TableName("crm_drawing_history")
public class CrmDrawingHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("drawing_id") private Long drawingId;
    @TableField("operation") private String operation;              // CREATE/UPDATE/ADD_VERSION/RELEASE/ARCHIVE/OBSOLETE
            @TableField("before_json") private String beforeJson;
    @TableField("after_json") private String afterJson;
    @TableField("changed_by") private Long changedBy;
    @TableField("changed_at") private LocalDateTime changedAt;
}
