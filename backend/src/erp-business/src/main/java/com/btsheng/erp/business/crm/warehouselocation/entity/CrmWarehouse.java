package com.btsheng.erp.business.crm.warehouselocation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "仓库主表（crm_warehouse）")
@TableName("crm_warehouse")
public class CrmWarehouse implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("warehouse_code")  private String warehouseCode;
    @TableField("warehouse_name")  private String warehouseName;
    @TableField("warehouse_type")  private String warehouseType;     // MAIN / SUB / LINE_SIDE
            @TableField("address")         private String address;
    @TableField("manager_user_id") private Long managerUserId;
    @TableField("is_active")       private Integer isActive = 1;
    @TableField("created_at")      private LocalDateTime createdAt;
}
