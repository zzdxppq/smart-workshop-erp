package com.btsheng.erp.production.outsource.incoming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.25 · 委外来料不良项
 */
@Data
@Schema(description = "来料不良项（crm_outsource_incoming_defect · Story 1.25）")
@TableName("crm_outsource_incoming_defect")
public class CrmOutsourceIncomingDefect implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("inspection_id") private Long inspectionId;
    @TableField("item_id")       private Long itemId;
    @TableField("defect_type")   private String defectType;
    @TableField("severity")      private String severity = "MINOR";
    @TableField("qty")           private Integer qty = 1;
    @TableField("description")   private String description;
    @TableField("created_at")    private LocalDateTime createdAt;
}
