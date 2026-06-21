package com.btsheng.erp.business.crm.outsourcecost.entity;

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
 * V1.3.7 · Story 1.26 · 委外成本归集（crm_outsource_cost_aggregation · FR-6-6）
 */
@Data
@Schema(description = "委外成本归集（crm_outsource_cost_aggregation · Story 1.26 FR-6-6）")
@TableName("crm_outsource_cost_aggregation")
public class CrmOutsourceCostAggregation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("outsource_id")     private Long outsourceId;
    @TableField("outsource_no")     private String outsourceNo;
    @TableField("material_code")    private String materialCode;
    @TableField("process_name")     private String processName;
    @TableField("cost_material")    private BigDecimal costMaterial = BigDecimal.ZERO;
    @TableField("cost_labor")       private BigDecimal costLabor = BigDecimal.ZERO;
    @TableField("cost_machine")     private BigDecimal costMachine = BigDecimal.ZERO;
    @TableField("cost_overhead")    private BigDecimal costOverhead = BigDecimal.ZERO;
    @TableField("cost_outsource")   private BigDecimal costOutsource = BigDecimal.ZERO;
    @TableField("cost_total")       private BigDecimal costTotal = BigDecimal.ZERO;
    @TableField("budget_cost")      private BigDecimal budgetCost = BigDecimal.ZERO;
    @TableField("deviation_pct")    private BigDecimal deviationPct;
    @TableField("deviation_level")  private String deviationLevel;
    @TableField("aggregation_scope") private String aggregationScope = "STEP";
    @TableField("remark")           private String remark;
    @TableField("created_by")       private Long createdBy;
    @TableField("created_at")       private LocalDateTime createdAt;
    @TableField("updated_at")       private LocalDateTime updatedAt;
}
