package com.btsheng.erp.business.crm.quote.cost.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("crm_quote_cost_item")
public class CrmQuoteCostItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("item_code") private String itemCode;
    @TableField("item_name") private String itemName;
    @TableField("billing_method") private String billingMethod;
    @TableField("unit") private String unit;
    @TableField("unit_price") private BigDecimal unitPrice;
    @TableField("profit_margin") private BigDecimal profitMargin;
    @TableField("process_code") private String processCode;
    @TableField("sort_order") private Integer sortOrder;
    @TableField("is_active") private Integer isActive = 1;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
