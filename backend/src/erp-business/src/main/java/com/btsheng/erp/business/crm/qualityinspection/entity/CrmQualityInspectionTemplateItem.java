package com.btsheng.erp.business.crm.qualityinspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("crm_quality_inspection_template_item")
public class CrmQualityInspectionTemplateItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("template_id") private Long templateId;
    @TableField("sort_order") private Integer sortOrder;
    @TableField("item_name") private String itemName;
    @TableField("standard") private String standard;
    @TableField("tolerance_upper") private String toleranceUpper;
    @TableField("tolerance_lower") private String toleranceLower;
    @TableField("severity") private String severity;
    @TableField("created_at") private LocalDateTime createdAt;
}
