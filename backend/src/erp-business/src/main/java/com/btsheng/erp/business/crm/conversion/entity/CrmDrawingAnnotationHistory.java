package com.btsheng.erp.business.crm.conversion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.8 · 标注历史 + 工程师工作量统计 hook
 */
@Data
@Schema(description = "标注历史（crm_drawing_annotation_history）")
@TableName("crm_drawing_annotation_history")
public class CrmDrawingAnnotationHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("annotation_id") private Long annotationId;
    @TableField("drawing_id") private Long drawingId;
    @TableField("operation") private String operation;   // CREATE/ARCHIVE
            @TableField("actor_user_id") private Long actorUserId;
    @TableField("snapshot") private String snapshot;
    @TableField("created_at") private LocalDateTime createdAt;
}
