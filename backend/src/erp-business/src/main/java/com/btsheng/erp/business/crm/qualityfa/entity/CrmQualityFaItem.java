package com.btsheng.erp.business.crm.qualityfa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.29 · FA 8 维度检验项目
 */
@Data
@Schema(description = "FA 8 维度检验项目")
@TableName("crm_quality_fa_item")
public class CrmQualityFaItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("fa_id")          private Long faId;
    @TableField("dimension")      private String dimension;
    @TableField("item_name")      private String itemName;
    @TableField("standard")       private String standard;
    @TableField("measured_value") private String measuredValue;
    @TableField("tolerance")      private String tolerance;
    @TableField("passed")         private Integer passed = 0;
    @TableField("created_at")     private LocalDateTime createdAt;
}
