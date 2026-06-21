package com.btsheng.erp.business.crm.reconcile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "对账明细（crm_reconcile_item）")
@TableName("crm_reconcile_item")
public class CrmReconcileItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("reconcile_id")         private Long reconcileId;
    @TableField("outsource_order_id")   private Long outsourceOrderId;
    @TableField("outsource_order_no")   private String outsourceOrderNo;
    @TableField("item_name")            private String itemName;
    @TableField("quantity")             private Integer quantity;
    @TableField("unit_price")           private BigDecimal unitPrice;
    @TableField("amount")               private BigDecimal amount;
    @TableField("vendor_amount")        private BigDecimal vendorAmount;
    @TableField("final_amount")         private BigDecimal finalAmount;
    @TableField("is_consistent")        private Integer isConsistent;
    @TableField("sort")                 private Integer sort = 0;
}
