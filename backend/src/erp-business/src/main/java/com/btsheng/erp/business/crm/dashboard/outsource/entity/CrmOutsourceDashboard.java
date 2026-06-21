package com.btsheng.erp.business.crm.dashboard.outsource.entity;

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
@Schema(description = "委外看板")
@TableName("crm_outsource_dashboard")
public class CrmOutsourceDashboard implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("dashboard_no")      private String dashboardNo;
    @TableField("outsource_no")      private String outsourceNo;
    @TableField("vendor_name")       private String vendorName;
    @TableField("status")            private String status;
    @TableField("metric_type")       private String metricType;
    @TableField("metric_name")       private String metricName;
    @TableField("metric_value")      private BigDecimal metricValue = BigDecimal.ZERO;
    @TableField("quality_pass_rate") private BigDecimal qualityPassRate;
    @TableField("alert_level")       private String alertLevel;
    @TableField("snapshot_at")       private LocalDateTime snapshotAt;
    @TableField("created_at")        private LocalDateTime createdAt;
}
