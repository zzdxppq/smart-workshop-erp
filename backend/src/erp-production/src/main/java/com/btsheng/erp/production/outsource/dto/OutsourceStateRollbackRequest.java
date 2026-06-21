package com.btsheng.erp.production.outsource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.22 · 委外状态机回退请求
 */
@Data
@Schema(description = "委外状态机回退请求（含 REJECTED 拒收路径）")
public class OutsourceStateRollbackRequest {

    @Schema(description = "委外单主键 ID", example = "1", required = true)
    private Long outsourceId;

    @Schema(description = "回退原因（必填）", example = "来料不良拒收", required = true)
    private String reason;

    @Schema(description = "操作员角色（V1.3.7 AD-1）", example = "品检")
    private String operatorRole;
}
