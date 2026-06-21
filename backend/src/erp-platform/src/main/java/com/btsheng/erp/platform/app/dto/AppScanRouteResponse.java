package com.btsheng.erp.platform.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "APP 扫码路由响应")
public class AppScanRouteResponse {
    @Schema(description = "扫码类型：WORK_ORDER/MATERIAL/FLOW/DEVICE/OUTSOURCE_ORDER/UNKNOWN")
    private String type;
    @Schema(description = "业务 ID（解析自 prefix）")
    private Long id;
    @Schema(description = "业务编码（解析自 prefix，物料/设备用）")
    private String code;
    @Schema(description = "路由 URL（前端按此跳 Activity）")
    private String routeUrl;
}
