package com.btsheng.erp.business.crm.dashboard.multidim.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "多维看板快照")
@TableName("crm_dashboard_snapshot")
public class CrmDashboardSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("snapshot_no")  private String snapshotNo;
    @TableField("dimension")    private String dimension;
    @TableField("metric_name")  private String metricName;
    @TableField("metric_value") private BigDecimal metricValue;
    @TableField("metric_unit")  private String metricUnit;
    @TableField("dim_dept")     private String dimDept;
    @TableField("dim_category") private String dimCategory;
    @TableField("dim_period")   private String dimPeriod;
    @TableField("snapshot_at")  private LocalDateTime snapshotAt;
    @TableField("created_at")   private LocalDateTime createdAt;
}
