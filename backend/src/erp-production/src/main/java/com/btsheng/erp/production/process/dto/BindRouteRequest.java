package com.btsheng.erp.production.process.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.10 · AC-3.4.3 工艺路线绑定图纸请求
 */
@Data
@Schema(description = "工艺路线绑定图纸请求（POST /processes/{id}/bind-to-drawing）")
public class BindRouteRequest {

    @Schema(description = "图纸 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long drawingId;

    @Schema(description = "工艺路线版本", example = "v1")
    private String version = "v1";

    @Schema(description = "变更原因（P2 修补：工艺变更历史）")
    private String changeReason;
}
