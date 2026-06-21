package com.btsheng.erp.business.crm.warehouselocation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 仓储批次视图实体（映射 V1.3.8 crm_batch + JOIN 展示字段）
 */
@Data
@Schema(description = "批次（crm_batch · V1.3.8）")
@TableName("crm_batch")
public class CrmBatch implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("batch_no")
    private String batchNo;
    @TableField("material_id")
    private Long materialId;
    @TableField("po_id")
    private Long poId;
    @TableField("po_item_id")
    private Long poItemId;
    @TableField("quantity")
    private Integer quantity;
    @TableField("arrived_at")
    private LocalDateTime arrivedAt;
    @TableField("quality_status")
    private String qualityStatus = "PENDING";
    @TableField("created_by")
    private Long createdBy;
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** JOIN crm_material */
    @TableField(exist = false)
    private String materialCode;
    /** JOIN crm_purchase_order */
    @TableField(exist = false)
    private Long supplierId;
    @TableField(exist = false)
    private String supplierName;
    /** V1.3.7 库位/FEFO 字段在新表结构中不存在，保留供前端兼容展示 */
    @TableField(exist = false)
    private String locationCode;
    @TableField(exist = false)
    private Integer fefoOrder;

    public Integer getQty() {
        return quantity;
    }

    public void setQty(Integer qty) {
        this.quantity = qty;
    }

    public LocalDateTime getReceivedAt() {
        return arrivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.arrivedAt = receivedAt;
    }
}
