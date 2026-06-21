package com.btsheng.erp.business.crm.warehouse.scan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "仓管扫码")
@TableName("crm_warehouse_incoming_scan")
public class CrmWarehouseIncomingScan implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("scan_no")       private String scanNo;
    @TableField("permission_no") private String permissionNo;
    @TableField("user_id")       private Long userId;
    @TableField("vendor_name")   private String vendorName;
    @TableField("outsource_no")  private String outsourceNo;
    @TableField("scan_type")     private String scanType;
    @TableField("scan_status")   private String scanStatus;
    @TableField("total_count")   private Integer totalCount = 0;
    @TableField("email")         private String email;
    @TableField("scan_time")     private LocalDateTime scanTime;
    @TableField("confirmed_at")  private LocalDateTime confirmedAt;
    @TableField("created_at")    private LocalDateTime createdAt;
}
