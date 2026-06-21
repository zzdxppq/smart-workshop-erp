package com.btsheng.erp.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 权限分配请求（V1.3.7）
 */
@Schema(description = "权限分配请求")
public class PermissionAssignRequest {

    @Schema(description = "菜单/资源 ID", example = "1001")
    private Long menuId;

    @Schema(description = "操作数组", example = "[\"view\", \"add\", \"edit\"]")
    private List<String> actions;

    public Long getMenuId() { return menuId; }
    public void setMenuId(Long menuId) { this.menuId = menuId; }
    public List<String> getActions() { return actions; }
    public void setActions(List<String> actions) { this.actions = actions; }
}
