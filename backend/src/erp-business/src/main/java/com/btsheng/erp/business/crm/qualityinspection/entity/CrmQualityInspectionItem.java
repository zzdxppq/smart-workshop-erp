package com.btsheng.erp.business.crm.qualityinspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.28 · 来料/过程/成品检项目
 */
@Data
@Schema(description = "来料/过程/成品检项目")
@TableName("crm_quality_inspection_item")
public class CrmQualityInspectionItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("inspection_id") private Long inspectionId;
    @TableField("item_name")     private String itemName;
    @TableField("standard")      private String standard;
    @TableField("measured_value") private String measuredValue;
    @TableField("severity")      private String severity = "INFO";
    @TableField("passed")        private Integer passed = 0;
    @TableField("defect_desc")   private String defectDesc;
    @TableField("created_at")    private LocalDateTime createdAt;
}
