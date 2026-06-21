package com.btsheng.erp.business.crm.warehousescan.entity;

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
@Schema(description = "仓库库位（crm_warehouse_location）")
@TableName("crm_warehouse_location")
public class CrmWarehouseLocation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("location_code") private String locationCode;
    @TableField("warehouse")     private String warehouse;
    @TableField("zone")          private String zone;
    @TableField("position")      private String position;
    @TableField("capacity")      private BigDecimal capacity;
    @TableField("is_active")     private Integer isActive = 1;
    @TableField("created_at")    private LocalDateTime createdAt;
}
