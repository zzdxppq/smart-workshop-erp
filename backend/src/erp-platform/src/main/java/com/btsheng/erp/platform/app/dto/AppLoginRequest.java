package com.btsheng.erp.platform.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "APP 登录请求")
public class AppLoginRequest {
    @Schema(description = "用户名", example = "zhangsan")
    private String username;
    @Schema(description = "密码", example = "Pass1234")
    private String password;
    @Schema(description = "设备 ID", example = "DEV-12345")
    private String deviceId;
    @Schema(description = "平台（iOS/Android）", example = "Android")
    private String platform;
    @Schema(description = "APP 版本", example = "1.0.0")
    private String appVersion;
    @Schema(description = "登录类型（PASSWORD/FINGERPRINT）", example = "PASSWORD")
    private String loginType;
}
