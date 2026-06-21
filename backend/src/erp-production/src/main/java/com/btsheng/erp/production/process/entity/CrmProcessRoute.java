package com.btsheng.erp.production.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.10 · 工艺路线（图纸关联）
 */
@Data
@Schema(description = "工艺路线（crm_process_route）")
@TableName("crm_process_route")
public class CrmProcessRoute implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("drawing_id") private Long drawingId;
    @TableField("drawing_no") private String drawingNo;
    @TableField("process_id") private Long processId;
    @TableField("process_code") private String processCode;
    @TableField("version") private String version = "v1";
    @TableField("status") private String status = "DRAFT";              // DRAFT/RELEASED/ARCHIVED
            @TableField("released_by") private Long releasedBy;
    @TableField("released_at") private LocalDateTime releasedAt;
    @TableField("change_reason") private String changeReason;            // P2 修补 3：变更历史
            @TableField("created_by") private Long createdBy;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
