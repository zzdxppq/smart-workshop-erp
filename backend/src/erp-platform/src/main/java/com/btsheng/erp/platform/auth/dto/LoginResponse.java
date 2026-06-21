package com.btsheng.erp.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 登录响应 DTO（V1.3.7）
 */
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "access token（2h）")
    private String accessToken;

    @Schema(description = "refresh token（7d）")
    private String refreshToken;

    @Schema(description = "过期秒数（access）")
    private long expiresIn;

    @Schema(description = "用户信息")
    private UserDto user;

    @Schema(description = "角色编码列表")
    private List<String> roles;

    @Schema(description = "权限列表（menu_code:action）")
    private List<String> permissions;

    @Schema(description = "可访问菜单路由 path 列表（DB sys_menu）")
    private List<String> menuPaths;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    public List<String> getMenuPaths() { return menuPaths; }
    public void setMenuPaths(List<String> menuPaths) { this.menuPaths = menuPaths; }
}
