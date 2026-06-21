package com.btsheng.erp.business.crm.materialbarcode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.11 · 物料条码扫码历史
 *
 * 记录 GENERATE / PARSE / INBOUND / OUTBOUND / VERIFY 5 种扫码事件
 * 用于扫码追溯 + 离线缓存同步（1.12 联动）
 */
@Data
@Schema(description = "物料条码扫码历史（crm_barcode_history）")
@TableName("crm_barcode_history")
public class CrmBarcodeHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("barcode_no")     private String barcodeNo;
    @TableField("scan_user_id")   private Long scanUserId;
    @TableField("scan_at")        private LocalDateTime scanAt;
    @TableField("scan_location")  private String scanLocation;
    @TableField("scan_type")      private String scanType;          // GENERATE / PARSE / INBOUND / OUTBOUND / VERIFY
            @TableField("scan_result")    private String scanResult = "SUCCESS";  // SUCCESS / FAILED
    @TableField("error_msg")      private String errorMsg;
    @TableField("client_type")    private String clientType = "WEB";  // WEB / ANDROID / IOS
            @TableField("remark")         private String remark;
}
