package com.btsheng.erp.business.crm.quote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.5 · AC-2.2.1 · 报价明细
 * V2.1 改造：新增工艺相关字段
 */
@Data
@Schema(description = "报价明细（crm_quote_item）")
@TableName("crm_quote_item")
public class CrmQuoteItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("quote_id") private Long quoteId;
    @TableField("drawing_no") private String drawingNo;
    @TableField("customer_drawing_no") private String customerDrawingNo;
    @TableField("drawing_id") private Long drawingId;
    @TableField("product_name") private String productName;
    @TableField("process_summary") private String processSummary;  // V2.1 工艺汇总JSON
    @TableField("process_route") private String processRoute;      // V2.1 工艺路线
    @TableField("total_hours") private BigDecimal totalHours;     // V2.1 总工时（小时）
    @TableField("template_id") private Long templateId;           // V2.1 引用的范本ID
    @TableField("surface_area") private BigDecimal surfaceArea;   // V2.1 表处面积（cm²）
    @TableField("anodize_area") private BigDecimal anodizeArea;
    @TableField("solid_solution_area") private BigDecimal solidSolutionArea;
    @TableField("forming_area") private BigDecimal formingArea;
    @TableField("material") private String material;
    @TableField("spec") private String spec;
    @TableField("unit_weight") private BigDecimal unitWeight;
    @TableField("quantity") private Integer quantity;
    @TableField("unit_price") private BigDecimal unitPrice;
    @TableField("amount") private BigDecimal amount;
    @TableField("is_fa") private Integer isFa = 0;
    @TableField("is_new") private Integer isNew = 0;
    @TableField("sort") private Integer sort = 0;
}
