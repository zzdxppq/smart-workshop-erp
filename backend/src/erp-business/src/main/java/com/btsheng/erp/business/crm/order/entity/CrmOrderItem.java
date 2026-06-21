package com.btsheng.erp.business.crm.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.6 · AC-2.3.1 · 订单明细
 *
 * <p>含 quantityAdjustment 字段（继承 1.5 hook） + producedQty / shippedQty 累计字段
 */
@Data
@Schema(description = "订单明细（crm_order_item）")
@TableName("crm_order_item")
public class CrmOrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_id") private Long orderId;
    @TableField("drawing_no") private String drawingNo;
    @TableField("customer_drawing_no") private String customerDrawingNo;  // V2.1 客户图号
    @TableField("drawing_id") private Long drawingId;                      // V2.1 关联图纸库
    @TableField("product_name") private String productName;              // V2.1 产品名称
    @TableField("process_route") private String processRoute;              // V2.1 工艺路线预览
    @TableField("material_no") private String materialNo;  // V2.1 料号，订单提交时生成
    @TableField("source_quotation_detail_id") private Long sourceQuotationDetailId;  // V2.1 来源报价明细行ID
    @TableField("material") private String material;
    @TableField("spec") private String spec;
    @TableField("unit_weight") private BigDecimal unitWeight;              // V2.1 单件重量(kg)
    @TableField("quantity") private Integer quantity;
    @TableField("unit_price") private BigDecimal unitPrice;
    @TableField("amount") private BigDecimal amount;
    @TableField("quantity_adjustment") private Integer quantityAdjustment = 0;
    @TableField("is_fa") private Integer isFa = 0;
    @TableField("is_new") private Integer isNew = 0;
    @TableField("sort") private Integer sort = 0;
    @TableField("produced_qty") private Integer producedQty = 0;
    @TableField("shipped_qty") private Integer shippedQty = 0;
}
