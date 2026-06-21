package com.btsheng.erp.business.crm.quality.pickup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "品质领料单")
@TableName("crm_quality_pickup")
public class CrmQualityPickup implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("pickup_no")       private String pickupNo;
    @TableField("scan_no")         private String scanNo;
    @TableField("inspector_id")    private Long inspectorId;
    @TableField("inspector_name")  private String inspectorName;
    @TableField("vendor_name")     private String vendorName;
    @TableField("pickup_type")     private String pickupType;
    @TableField("inspect_status")  private String inspectStatus;
    @TableField("total_count")     private Integer totalCount = 0;
    @TableField("pass_count")      private Integer passCount = 0;
    @TableField("fail_count")      private Integer failCount = 0;
    @TableField("email")           private String email;
    @TableField("pickup_time")     private LocalDateTime pickupTime;
    @TableField("inspected_at")    private LocalDateTime inspectedAt;
    @TableField("created_at")      private LocalDateTime createdAt;
}
