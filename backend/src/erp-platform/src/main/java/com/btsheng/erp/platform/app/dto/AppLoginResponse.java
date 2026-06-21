package com.btsheng.erp.platform.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "APP 登录响应")
public class AppLoginResponse {
    @Schema(description = "access_token (2h)")
    private String accessToken;
    @Schema(description = "refresh_token (7d)")
    private String refreshToken;
    @Schema(description = "jti")
    private String jti;
    @Schema(description = "用户 ID", example = "10086")
    private Long userId;
    @Schema(description = "角色列表")
    private java.util.List<String> roles;
    @Schema(description = "权限列表")
    private java.util.List<String> permissions;
}
