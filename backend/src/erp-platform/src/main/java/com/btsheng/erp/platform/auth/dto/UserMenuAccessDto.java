package com.btsheng.erp.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "用户菜单访问权限")
public class UserMenuAccessDto {

    @Schema(description = "可访问路由 path 列表")
    private List<String> menuPaths = new ArrayList<>();

    @Schema(description = "权限码 menu_code:action")
    private List<String> permissions = new ArrayList<>();

    @Schema(description = "菜单树（管理端展示用）")
    private List<Map<String, Object>> menus = new ArrayList<>();

    public List<String> getMenuPaths() { return menuPaths; }
    public void setMenuPaths(List<String> menuPaths) { this.menuPaths = menuPaths; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    public List<Map<String, Object>> getMenus() { return menus; }
    public void setMenus(List<Map<String, Object>> menus) { this.menus = menus; }
}
