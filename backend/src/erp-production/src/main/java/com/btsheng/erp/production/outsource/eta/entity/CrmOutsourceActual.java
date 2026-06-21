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
 * V1.3.7 · Story 1.24 · 委外实际交期历史（crm_outsource_actual · FR-6-4）
 */
@Data
@Schema(description = "委外实际交期历史（crm_outsource_actual · Story 1.24 FR-6-4）")
@TableName("crm_outsource_actual")
public class CrmOutsourceActual implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("eta_id")           private Long etaId;
    @TableField("outsource_id")     private Long outsourceId;
    @TableField("outsource_no")     private String outsourceNo;
    @TableField("supplier_id")      private Long supplierId;
    @TableField("supplier_name")    private String supplierName;
    @TableField("process_name")     private String processName;
    @TableField("qty")              private Integer qty = 1;
    @TableField("promised_date")    private LocalDate promisedDate;
    @TableField("actual_date")      private LocalDate actualDate;
    @TableField("actual_days")      private Integer actualDays;
    @TableField("predicted_days")   private Integer predictedDays;
    @TableField("deviation_pct")    private BigDecimal deviationPct;
    @TableField("on_time")          private Integer onTime = 1;
    @TableField("created_at")       private LocalDateTime createdAt;
}
