package com.btsheng.erp.business.crm.reconcile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "月度对账单（crm_reconcile）")
@TableName("crm_reconcile")
public class CrmReconcile implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("reconcile_no")   private String reconcileNo;
    @TableField("vendor_id")      private Long vendorId;
    @TableField("vendor_name")    private String vendorName;
    @TableField("period_year")    private Integer periodYear;
    @TableField("period_month")   private Integer periodMonth;
    @TableField("total_amount")   private BigDecimal totalAmount = BigDecimal.ZERO;
    @TableField("status")         private String status = "DRAFT";
    @TableField("current_step")   private Integer currentStep = 1;
    @TableField("is_locked")      private Integer isLocked = 0;
    @TableField("created_by")     private Long createdBy;
    @TableField("created_at")     private LocalDateTime createdAt;
    @TableField("updated_at")     private LocalDateTime updatedAt;
}
