package com.btsheng.erp.business.crm.conversion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.8 · P2 修补：工程师工作量统计 hook
 */
@Data
@Schema(description = "工程师工作量（crm_engineer_workload）")
@TableName("crm_engineer_workload")
public class CrmEngineerWorkload implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id") private Long userId;
    @TableField("user_name") private String userName;
    @TableField("work_date") private LocalDate workDate;
    @TableField("annotation_count") private Integer annotationCount = 0;
    @TableField("conversion_count") private Integer conversionCount = 0;
    @TableField("drawing_created_count") private Integer drawingCreatedCount = 0;
}
