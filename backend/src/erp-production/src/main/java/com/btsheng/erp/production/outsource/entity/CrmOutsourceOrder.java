package com.btsheng.erp.production.outsource.entity;

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

@Data
@Schema(description = "委外单主表（crm_outsource_order）")
@TableName("crm_outsource_order")
public class CrmOutsourceOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("outsource_no")      private String outsourceNo;
    @TableField("workorder_no")      private String workorderNo;
    @TableField("step_no")           private Integer stepNo;
    @TableField("supplier_id")       private Long supplierId;
    @TableField("supplier_name")     private String supplierName;
    @TableField("process_name")      private String processName;
    @TableField("material_code")     private String materialCode;
    @TableField("drawing_id")        private Long drawingId;
    @TableField("qty")               private Integer qty = 0;
    @TableField("unit_price")        private BigDecimal unitPrice = BigDecimal.ZERO;
    @TableField("total_amount")      private BigDecimal totalAmount = BigDecimal.ZERO;
    @TableField("delivery_date")     private LocalDate deliveryDate;
    @TableField("status")            private String status = "DRAFT";
    @TableField("rework_count")      private Integer reworkCount = 0;
    @TableField("creator_user_id")   private Long creatorUserId;
    @TableField("submitted_at")      private LocalDateTime submittedAt;
    @TableField("accepted_at")       private LocalDateTime acceptedAt;
    @TableField("completed_at")      private LocalDateTime completedAt;
    @TableField("closed_at")         private LocalDateTime closedAt;
    @TableField("is_urgent")         private Integer isUrgent = 0;
    @TableField("remark")            private String remark;
    @TableField("created_at")        private LocalDateTime createdAt;
    @TableField("updated_at")        private LocalDateTime updatedAt;
}
