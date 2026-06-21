package com.btsheng.erp.business.crm.warehouse.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "仓管扫码权限")
@TableName("crm_warehouse_incoming_permission")
public class CrmWarehouseIncomingPermission implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("permission_no")     private String permissionNo;
    @TableField("user_id")           private Long userId;
    @TableField("user_name")         private String userName;
    @TableField("role")              private String role;
    @TableField("permission_type")   private String permissionType;
    @TableField("valid_from")        private LocalDateTime validFrom;
    @TableField("valid_to")          private LocalDateTime validTo;
    @TableField("granted_by")        private String grantedBy;
    @TableField("status")            private String status;
    @TableField("email")             private String email;
    @TableField("created_at")        private LocalDateTime createdAt;
}
