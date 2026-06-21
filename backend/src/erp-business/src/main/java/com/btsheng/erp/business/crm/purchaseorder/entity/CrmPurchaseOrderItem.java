package com.btsheng.erp.business.crm.purchaseorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("crm_purchase_order_item")
public class CrmPurchaseOrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("po_id")
    private Long poId;

    @TableField("purchase_order_id")
    private Long purchaseOrderId;

    @TableField("material_id")
    private Long materialId;

    @TableField("material_code")
    private String materialCode;

    @TableField("material_name")
    private String materialName;

    @TableField("quantity")
    private Integer quantity;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("delivery_date")
    private LocalDate deliveryDate;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
