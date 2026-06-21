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
 * V1.3.7 · Story 1.27 · 委外检验项目
 */
@Data
@Schema(description = "委外检验项目（crm_outsource_quality_item · Story 1.27）")
@TableName("crm_outsource_quality_item")
public class CrmOutsourceQualityItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("quality_id")      private Long qualityId;
    @TableField("item_type")       private String itemType = "FA";
    @TableField("item_name")       private String itemName;
    @TableField("standard")        private String standard;
    @TableField("measured_value")  private String measuredValue;
    @TableField("tolerance")       private String tolerance;
    @TableField("passed")          private Integer passed = 0;
    @TableField("created_at")      private LocalDateTime createdAt;
}
