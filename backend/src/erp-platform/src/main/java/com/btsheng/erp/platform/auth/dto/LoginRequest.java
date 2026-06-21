package com.btsheng.erp.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求 DTO（V1.3.7）
 */
@Schema(description = "登录请求")
public class LoginRequest {

    @NotBlank
    @Size(min = 1, max = 64)
    @Schema(description = "登录名", example = "admin")
    private String username;

    @NotBlank
    @Size(min = 1, max = 128)
    @Schema(description = "密码", example = "123456")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
