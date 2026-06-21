package com.btsheng.erp.business.crm.dashboard.production.entity;

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
@Schema(description = "生产看板快照")
@TableName("crm_dashboard_production")
public class CrmDashboardProduction implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("snapshot_no")     private String snapshotNo;
    @TableField("workorder_no")    private String workorderNo;
    @TableField("product_name")    private String productName;
    @TableField("workorder_status") private String workorderStatus;
    @TableField("qty_planned")     private Integer qtyPlanned = 0;
    @TableField("qty_completed")   private Integer qtyCompleted = 0;
    @TableField("progress")        private BigDecimal progress = BigDecimal.ZERO;
    @TableField("alert_type")      private String alertType;
    @TableField("alert_message")   private String alertMessage;
    @TableField("snapshot_at")     private LocalDateTime snapshotAt;
    @TableField("created_at")      private LocalDateTime createdAt;
}
