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
 * V1.3.7 · Story 1.28 · AQL 抽样记录
 */
@Data
@Schema(description = "AQL 抽样记录")
@TableName("crm_quality_sample")
public class CrmQualitySample implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("inspection_id") private Long inspectionId;
    @TableField("sample_no")     private String sampleNo;
    @TableField("item_id")       private Long itemId;
    @TableField("sample_qty")    private Integer sampleQty = 1;
    @TableField("defect_qty")    private Integer defectQty = 0;
    @TableField("aql_passed")    private Integer aqlPassed = 0;
    @TableField("remark")        private String remark;
    @TableField("created_at")    private LocalDateTime createdAt;
}
