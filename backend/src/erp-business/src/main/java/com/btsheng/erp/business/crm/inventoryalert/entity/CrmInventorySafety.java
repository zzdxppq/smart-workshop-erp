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
@Schema(description = "安全库存配置（crm_inventory_safety）")
@TableName("crm_inventory_safety")
public class CrmInventorySafety implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("material_code")  private String materialCode;
    @TableField("material_name")  private String materialName;
    @TableField("min_qty")        private Integer minQty = 0;
    @TableField("max_qty")        private Integer maxQty = 0;
    @TableField("reorder_qty")    private Integer reorderQty = 0;
    @TableField("unit")           private String unit = "个";
    @TableField("current_qty")    private Integer currentQty = 0;
    @TableField("enabled")        private Integer enabled = 1;
    @TableField("owner_user_id")  private Long ownerUserId;
    @TableField("remark")         private String remark;
    @TableField("created_at")     private LocalDateTime createdAt;
    @TableField("updated_at")     private LocalDateTime updatedAt;
}
