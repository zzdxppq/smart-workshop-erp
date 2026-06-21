package com.btsheng.erp.business.crm.warehousescan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "APP 扫码出入库记录（crm_warehouse_scan）")
@TableName("crm_warehouse_scan")
public class CrmWarehouseScan implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("scan_no")            private String scanNo;
    @TableField("scan_type")          private String scanType;
    @TableField("barcode_no")         private String barcodeNo;
    @TableField("material_code")      private String materialCode;
    @TableField("location_code")      private String locationCode;
    @TableField("qty")                private Integer qty;
    @TableField("workorder_no")       private String workorderNo;
    @TableField("batch_no")           private String batchNo;
    @TableField("client_id")          private String clientId;
    @TableField("sync_status")        private String syncStatus = "SYNCED";
    @TableField("scanned_by")         private Long scannedBy;
    @TableField("scanned_at")         private LocalDateTime scannedAt;
    @TableField("synced_at")          private LocalDateTime syncedAt;
    @TableField("conflict_type")      private String conflictType;
    @TableField("conflict_resolution") private String conflictResolution;
    @TableField("remark")             private String remark;
}
