package com.btsheng.erp.business.crm.outsourcecost.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.26 · 委外成本归集请求
 */
@Data
@Schema(description = "委外成本归集请求（Story 1.26 FR-6-6）")
public class AggregateCostRequest {

    @Schema(description = "委外单 ID", example = "1", required = true)
    private Long outsourceId;

    @Schema(description = "物料编码", example = "ZZ-0002", required = true)
    private String materialCode;

    @Schema(description = "工序名称", example = "调质")
    private String processName;

    @Schema(description = "材料成本", example = "100.00", required = true)
    private BigDecimal costMaterial = BigDecimal.ZERO;

    @Schema(description = "人工成本", example = "30.00", required = true)
    private BigDecimal costLabor = BigDecimal.ZERO;

    @Schema(description = "设备成本", example = "20.00", required = true)
    private BigDecimal costMachine = BigDecimal.ZERO;

    @Schema(description = "管理成本", example = "10.00", required = true)
    private BigDecimal costOverhead = BigDecimal.ZERO;

    @Schema(description = "委外成本", example = "150.00", required = true)
    private BigDecimal costOutsource = BigDecimal.ZERO;

    @Schema(description = "预算成本（用于偏差率）", example = "300.00", required = true)
    private BigDecimal budgetCost = BigDecimal.ZERO;

    @Schema(description = "归集范围 STEP/PROCESS/WHOLE", example = "PROCESS")
    private String aggregationScope = "PROCESS";
}
