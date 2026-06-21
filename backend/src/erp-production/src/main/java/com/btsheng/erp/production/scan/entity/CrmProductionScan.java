package com.btsheng.erp.production.scan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "生产扫码（crm_production_scan）")
@TableName("crm_production_scan")
public class CrmProductionScan implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("scan_no")          private String scanNo;
    @TableField("workorder_no")     private String workorderNo;
    @TableField("scan_type")        private String scanType;          // START / REPORT / STATION
            @TableField("operator_user_id") private Long operatorUserId;
    @TableField("equipment_id")     private Long equipmentId;
    @TableField("qty")              private Integer qty = 0;
    @TableField("step_no")          private Integer stepNo;
    @TableField("scanned_at")       private LocalDateTime scannedAt;
    @TableField("client_id")        private String clientId;
    @TableField("sync_status")      private String syncStatus = "SYNCED";
    @TableField("remark")           private String remark;
}
