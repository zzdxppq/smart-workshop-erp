package com.btsheng.erp.production.outsource.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.27 · 委外不良项
 */
@Data
@Schema(description = "委外不良项（crm_outsource_quality_defect · Story 1.27）")
@TableName("crm_outsource_quality_defect")
public class CrmOutsourceQualityDefect implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("quality_id")   private Long qualityId;
    @TableField("item_id")      private Long itemId;
    @TableField("defect_type")  private String defectType;
    @TableField("severity")     private String severity = "MINOR";
    @TableField("qty")          private Integer qty = 1;
    @TableField("description")  private String description;
    @TableField("created_at")   private LocalDateTime createdAt;
}
