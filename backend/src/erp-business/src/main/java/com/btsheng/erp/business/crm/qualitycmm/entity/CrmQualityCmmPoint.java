package com.btsheng.erp.business.crm.qualitycmm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.30 · CMM 测点
 */
@Data
@Schema(description = "CMM 测点")
@TableName("crm_quality_cmm_point")
public class CrmQualityCmmPoint implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("cmm_id")          private Long cmmId;
    @TableField("point_no")        private String pointNo;
    @TableField("axis")            private String axis = "X";
    @TableField("nominal_value")   private BigDecimal nominalValue;
    @TableField("measured_value")  private BigDecimal measuredValue;
    @TableField("tolerance_upper") private BigDecimal toleranceUpper;
    @TableField("tolerance_lower") private BigDecimal toleranceLower;
    @TableField("deviation")       private BigDecimal deviation;
    @TableField("passed")          private Integer passed = 0;
    @TableField("created_at")      private LocalDateTime createdAt;
}
