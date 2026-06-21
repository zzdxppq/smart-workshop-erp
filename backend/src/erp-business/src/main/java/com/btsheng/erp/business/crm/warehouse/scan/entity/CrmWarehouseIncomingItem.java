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
@Schema(description = "仓管扫码明细")
@TableName("crm_warehouse_incoming_item")
public class CrmWarehouseIncomingItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("item_no")           private String itemNo;
    @TableField("scan_no")           private String scanNo;
    @TableField("barcode")           private String barcode;
    @TableField("barcode_type")      private String barcodeType;
    @TableField("material_code")     private String materialCode;
    @TableField("material_name")     private String materialName;
    @TableField("quantity")          private Integer quantity = 0;
    @TableField("batch_no")          private String batchNo;
    @TableField("warehouse_location") private String warehouseLocation;
    @TableField("remark")            private String remark;
    @TableField("created_at")        private LocalDateTime createdAt;
}
