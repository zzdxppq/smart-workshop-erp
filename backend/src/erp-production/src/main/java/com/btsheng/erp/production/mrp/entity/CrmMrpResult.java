package com.btsheng.erp.production.mrp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "MRP 结果（crm_mrp_result）")
@TableName("crm_mrp_result")
public class CrmMrpResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("run_id")                private Long runId;
    @TableField("material_code")         private String materialCode;
    @TableField("material_name")         private String materialName;
    @TableField("required_qty")          private Integer requiredQty;
    @TableField("current_stock")         private Integer currentStock = 0;
    @TableField("on_order_qty")          private Integer onOrderQty = 0;
    @TableField("shortage_qty")          private Integer shortageQty = 0;
    @TableField("purchase_suggestion")   private Integer purchaseSuggestion = 0;
    @TableField("supplier_id")           private Long supplierId;
    @TableField("unit_cost")             private BigDecimal unitCost = BigDecimal.ZERO;
    @TableField("total_cost")            private BigDecimal totalCost = BigDecimal.ZERO;
}
