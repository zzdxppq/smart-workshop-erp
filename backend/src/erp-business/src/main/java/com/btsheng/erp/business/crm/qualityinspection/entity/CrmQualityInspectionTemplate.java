package com.btsheng.erp.business.crm.qualityinspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("crm_quality_inspection_template")
public class CrmQualityInspectionTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("template_no") private String templateNo;
    @TableField("template_name") private String templateName;
    @TableField("drawing_no_pattern") private String drawingNoPattern;
    @TableField("material_code") private String materialCode;
    @TableField("inspection_type") private String inspectionType;
    @TableField("sample_ratio") private BigDecimal sampleRatio;
    @TableField("status") private String status;
    @TableField("version") private Integer version;
    @TableField("remark") private String remark;
    @TableField("published_by") private Long publishedBy;
    @TableField("published_at") private LocalDateTime publishedAt;
    @TableField("archived_by") private Long archivedBy;
    @TableField("archived_at") private LocalDateTime archivedAt;
    @TableField("created_by") private Long createdBy;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
