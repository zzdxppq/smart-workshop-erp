package com.btsheng.erp.production.outsource.eta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.24 · 委外交期预估请求
 */
@Data
@Schema(description = "委外交期预估请求（Story 1.24 FR-6-4）")
public class PredictEtaRequest {

    @Schema(description = "委外单主键 ID", example = "1", required = true)
    private Long outsourceId;

    @Schema(description = "供应商 ID（可选 · 缺省从委外单读取）", example = "101")
    private Long supplierId;

    @Schema(description = "工序名称（可选）", example = "调质")
    private String processName;

    @Schema(description = "数量（可选 · 缺省从委外单读取）", example = "10")
    private Integer qty;

    @Schema(description = "起始日期（缺省 = today）", example = "2026-06-12")
    private LocalDate startDate;
}
