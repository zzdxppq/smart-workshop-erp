package com.btsheng.erp.production.process.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.10 · AC-3.4.2 新增工序请求
 */
@Data
@Schema(description = "新增工序请求（POST /processes/{id}/steps）")
public class AddStepRequest {

    @Schema(description = "工序名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stepName;

    @Schema(description = "5 段：原材料/粗加工/精加工/表面处理/检验", example = "原材料")
    private String segment = "原材料";

    @Schema(description = "机器类型")
    private String machineType;

    @Schema(description = "具体机器 ID")
    private Long machineId;

    @Schema(description = "工时（必须 ≥ 0 · P1 修补 3）", example = "1.0")
    private BigDecimal estimatedHours = BigDecimal.ZERO;

    @Schema(description = "单价", example = "100.00")
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "是否为质检工序")
    private Boolean isQualityCheck = false;
}
