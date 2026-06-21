package com.btsheng.erp.platform.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "APP 扫码路由请求（query string: ?code=...）")
public class AppScanRouteRequest {
    @Schema(description = "扫码内容", example = "GD-20260610-0001")
    private String code;
}
