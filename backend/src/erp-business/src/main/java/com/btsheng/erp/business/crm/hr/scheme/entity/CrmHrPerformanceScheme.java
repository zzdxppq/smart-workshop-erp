package com.btsheng.erp.business.crm.hr.scheme.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("crm_hr_performance_scheme")
public class CrmHrPerformanceScheme implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("scheme_name") private String schemeName;
    @TableField("position") private String position;
    @TableField("output_weight") private BigDecimal outputWeight;
    @TableField("quality_weight") private BigDecimal qualityWeight;
    @TableField("attendance_weight") private BigDecimal attendanceWeight;
    @TableField("is_default") private Integer isDefault;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
