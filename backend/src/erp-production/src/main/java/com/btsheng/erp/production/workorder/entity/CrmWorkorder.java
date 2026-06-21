package com.btsheng.erp.production.workorder.entity;

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
@Schema(description = "工单主表（crm_workorder）")
@TableName("crm_workorder")
public class CrmWorkorder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("workorder_no")      private String workorderNo;
    @TableField("drawing_id")        private Long drawingId;
    @TableField("bom_id")            private Long bomId;
    @TableField("process_route_id")  private Long processRouteId;
    @TableField("material_code")     private String materialCode;
    @TableField("product_name")      private String productName;
    @TableField("qty")               private Integer qty = 1;
    @TableField("unit")              private String unit = "台";
    @TableField("priority")          private Integer priority = 5;
    @TableField("status")            private String status = "DRAFT";
    @TableField("scheduled_start")   private LocalDateTime scheduledStart;
    @TableField("scheduled_end")     private LocalDateTime scheduledEnd;
    @TableField("actual_start")      private LocalDateTime actualStart;
    @TableField("actual_end")        private LocalDateTime actualEnd;
    @TableField("equipment_id")      private Long equipmentId;
    @TableField("equipment_type")    private String equipmentType;
    @TableField("estimated_hours")   private BigDecimal estimatedHours = BigDecimal.ZERO;
    @TableField("actual_hours")      private BigDecimal actualHours = BigDecimal.ZERO;
    @TableField("is_fa")             private Integer isFa = 0;
    @TableField("created_by")        private Long createdBy;
    @TableField("owner_user_id")     private Long ownerUserId;
    @TableField("dept_id")           private Long deptId = 10L;
    @TableField("remark")            private String remark;
    @TableField("sales_order_id")    private Long salesOrderId;
    @TableField("sales_order_no")    private String salesOrderNo;
    @TableField("created_at")        private LocalDateTime createdAt;
    @TableField("updated_at")        private LocalDateTime updatedAt;
}
