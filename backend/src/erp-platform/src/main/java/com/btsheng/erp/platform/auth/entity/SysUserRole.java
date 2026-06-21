package com.btsheng.erp.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 用户-角色关联（V1.3.7）
 *
 * <p>DB 表 {@code sys_user_role}，联合主键 {@code (user_id, role_id)}。
 */
@Schema(description = "用户-角色关联")
@TableName("sys_user_role")
public class SysUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "角色 ID")
    @TableField("role_id")
    private Long roleId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
