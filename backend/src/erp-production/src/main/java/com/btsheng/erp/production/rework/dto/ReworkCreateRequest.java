package com.btsheng.erp.production.rework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.23 · 返修单创建请求
 */
@Data
@Schema(description = "返修单创建请求（Story 1.23 FR-6-3）")
public class ReworkCreateRequest {

    @Schema(description = "委外单主键 ID", example = "1", required = true)
    private Long outsourceId;

    @Schema(description = "返修原因（必填 · P1 修补 2）", example = "调质硬度不达标", required = true)
    private String reason;

    @Schema(description = "返修成本（P1 修补 3 · ≥ 0 · 计入月度对账）", example = "200.00")
    private BigDecimal cost = BigDecimal.ZERO;
}
