package com.btsheng.erp.business.finance.materialcost.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.40 · 财务·料号成本聚合视图（crm_material_cost_aggregation · FR-9-5 V1.3.4 新增 · P0）
 *
 * <p>5 段：materialAmount / processAmount / outsourceAmount / manageAmount / depreciationAmount
 * <p>跨模块：BOM(1.9) + 工艺(1.10) + 工单(1.15) + 委外(1.18/1.26) + 库存(1.14)
 */
@Data
@Schema(description = "料号成本聚合视图（Story 1.40 FR-9-5 V1.3.4 强化）")
@TableName("crm_material_cost_aggregation")
public class CrmMaterialCostAggregation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("agg_no")              private String aggNo;
    @TableField("material_id")         private Long materialId;
    @TableField("material_code")       private String materialCode;
    @TableField("material_name")       private String materialName;
    @TableField("agg_month")           private String aggMonth;
    @TableField("vendor_id")           private Long vendorId;
    @TableField("vendor_name")         private String vendorName;
    @TableField("qty")                 private BigDecimal qty;
    @TableField("material_amount")     private BigDecimal materialAmount;
    @TableField("process_amount")      private BigDecimal processAmount;
    @TableField("outsource_amount")    private BigDecimal outsourceAmount;
    @TableField("manage_amount")       private BigDecimal manageAmount;
    @TableField("depreciation_amount") private BigDecimal depreciationAmount;
    @TableField("total_cost")          private BigDecimal totalCost;
    @TableField("unit_cost")           private BigDecimal unitCost;
    @TableField("cost_sources")        private String costSources;
    @TableField("created_at")          private LocalDateTime createdAt;
    @TableField("updated_at")          private LocalDateTime updatedAt;
}
