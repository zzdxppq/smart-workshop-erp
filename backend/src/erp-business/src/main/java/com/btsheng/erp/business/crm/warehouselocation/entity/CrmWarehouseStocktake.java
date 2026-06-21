package com.btsheng.erp.business.crm.warehouselocation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "仓库盘点单（crm_warehouse_stocktake）")
@TableName("crm_warehouse_stocktake")
public class CrmWarehouseStocktake implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("stocktake_no")   private String stocktakeNo;
    @TableField("warehouse_code") private String warehouseCode;
    @TableField("status")         private String status = "DRAFT";
    @TableField("created_by")     private Long createdBy;
    @TableField("created_at")     private LocalDateTime createdAt;
    @TableField("updated_at")     private LocalDateTime updatedAt;
}
