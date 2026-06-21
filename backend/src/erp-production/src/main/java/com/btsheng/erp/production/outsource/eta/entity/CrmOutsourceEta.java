package com.btsheng.erp.production.outsource.eta.entity;

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
 * V1.3.7 · Story 1.24 · 委外预估交期（crm_outsource_eta · FR-6-4）
 */
@Data
@Schema(description = "委外预估交期（crm_outsource_eta · Story 1.24 FR-6-4）")
@TableName("crm_outsource_eta")
public class CrmOutsourceEta implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("eta_no")                  private String etaNo;
    @TableField("outsource_id")            private Long outsourceId;
    @TableField("outsource_no")            private String outsourceNo;
    @TableField("supplier_id")             private Long supplierId;
    @TableField("supplier_name")           private String supplierName;
    @TableField("process_name")            private String processName;
    @TableField("qty")                     private Integer qty = 1;
    @TableField("predicted_days")          private Integer predictedDays;
    @TableField("predicted_delivery_date") private LocalDate predictedDeliveryDate;
    @TableField("confidence")              private BigDecimal confidence = new BigDecimal("0.80");
    @TableField("base_samples")            private Integer baseSamples = 0;
    @TableField("actual_delivery_date")    private LocalDate actualDeliveryDate;
    @TableField("deviation_pct")           private BigDecimal deviationPct;
    @TableField("accuracy_passed")         private Integer accuracyPassed;
    @TableField("status")                  private String status = "PREDICTED";
    @TableField("created_by")              private Long createdBy;
    @TableField("created_at")              private LocalDateTime createdAt;
    @TableField("updated_at")              private LocalDateTime updatedAt;
}
