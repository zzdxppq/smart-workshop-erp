package com.btsheng.erp.business.crm.inventoryalert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "库存预警记录（crm_inventory_alert）")
@TableName("crm_inventory_alert")
public class CrmInventoryAlert implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("material_code")    private String materialCode;
    @TableField("alert_level")      private String alertLevel;
    @TableField("current_qty")      private Integer currentQty;
    @TableField("min_qty")          private Integer minQty;
    @TableField("message")          private String message;
    @TableField("status")           private String status = "OPEN";
    @TableField("triggered_at")     private LocalDateTime triggeredAt;
    @TableField("resolved_at")      private LocalDateTime resolvedAt;
    @TableField("resolved_by")      private Long resolvedBy;
    @TableField("resolution_note")  private String resolutionNote;
    @TableField("notified")         private Integer notified = 0;
}
