package com.btsheng.erp.business.crm.quality.pickup.entity;

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
@Schema(description = "品质领料明细")
@TableName("crm_quality_pickup_item")
public class CrmQualityPickupItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("pickup_item_no")   private String pickupItemNo;
    @TableField("pickup_no")        private String pickupNo;
    @TableField("material_code")    private String materialCode;
    @TableField("material_name")    private String materialName;
    @TableField("quantity")         private Integer quantity = 0;
    @TableField("inspect_result")   private String inspectResult;
    @TableField("defect_desc")      private String defectDesc;
    @TableField("measure_value")    private BigDecimal measureValue;
    @TableField("inspect_time")     private LocalDateTime inspectTime;
    @TableField("created_at")       private LocalDateTime createdAt;
}
