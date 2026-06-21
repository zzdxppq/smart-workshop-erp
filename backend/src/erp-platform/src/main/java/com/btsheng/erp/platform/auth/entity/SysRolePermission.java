package com.btsheng.erp.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 角色权限（V1.3.7）
 *
 * <p>DB 表 {@code sys_role_permission}，联合主键 {@code (role_id, menu_id, action)}。
 */
@Schema(description = "角色权限")
@TableName("sys_role_permission")
public class SysRolePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色 ID")
    @TableField("role_id")
    private Long roleId;

    @Schema(description = "菜单/资源 ID")
    @TableField("menu_id")
    private Long menuId;

    @Schema(description = "操作（view/add/edit/delete/audit/export/print）")
    @TableField("action")
    private String action;

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getMenuId() { return menuId; }
    public void setMenuId(Long menuId) { this.menuId = menuId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
