package com.btsheng.erp.production.outsource.eta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.24 · 委外实际交期更新请求
 */
@Data
@Schema(description = "委外实际交期更新请求（Story 1.24 FR-6-4）")
public class UpdateActualEtaRequest {

    @Schema(description = "预估单 ID", example = "1", required = true)
    private Long etaId;

    @Schema(description = "实际交付日期", example = "2026-06-25", required = true)
    private LocalDate actualDeliveryDate;
}
