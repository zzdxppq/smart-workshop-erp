package com.btsheng.erp.business.finance.cost.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.37 · 财务·成本核算单（crm_cost_accounting · FR-9-2）
 */
@Data
@Schema(description = "成本核算单（Story 1.37 FR-9-2）")
@TableName("crm_cost_accounting")
public class CrmCostAccounting implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("cost_no")        private String costNo;
    @TableField("ref_type")       private String refType;
    @TableField("ref_id")         private Long refId;
    @TableField("ref_no")         private String refNo;
    @TableField("material_id")    private Long materialId;
    @TableField("material_code")  private String materialCode;
    @TableField("material_name")  private String materialName;
    @TableField("qty")            private BigDecimal qty;
    @TableField("unit_cost")      private BigDecimal unitCost;
    @TableField("total_cost")     private BigDecimal totalCost;
    @TableField("standard_cost")  private BigDecimal standardCost;
    @TableField("variance")       private BigDecimal variance;
    @TableField("variance_rate")  private BigDecimal varianceRate;
    @TableField("status")         private String status = "DRAFT";
    @TableField("cost_date")      private LocalDate costDate;
    @TableField("created_at")     private LocalDateTime createdAt;
    @TableField("updated_at")     private LocalDateTime updatedAt;
}
