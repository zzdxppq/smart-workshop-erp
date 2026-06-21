package com.btsheng.erp.business.finance.cost.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.37 · 5 段成本归集请求
 */
@Data
@Schema(description = "5 段成本归集请求（Story 1.37 FR-9-2）")
public class AggregateCostRequest {
    @Schema(description = "引用类型 ORDER/WORKORDER/OUTSOURCE", required = true) private String refType;
    @Schema(description = "引用 ID", required = true) private Long refId;
    @Schema(description = "引用单号", required = true) private String refNo;
    @Schema(description = "物料 ID") private Long materialId;
    @Schema(description = "物料编码") private String materialCode;
    @Schema(description = "物料名称") private String materialName;
    @Schema(description = "数量", required = true) private BigDecimal qty;
    @Schema(description = "标准成本（用于偏差率计算）") private BigDecimal standardCost;
    @Schema(description = "成本日期", required = true) private LocalDate costDate;

    @Schema(description = "材料段金额", required = true) private BigDecimal materialAmount;
    @Schema(description = "加工段金额", required = true) private BigDecimal processAmount;
    @Schema(description = "委外段金额", required = true) private BigDecimal outsourceAmount;
    @Schema(description = "管理段金额", required = true) private BigDecimal manageAmount;
    @Schema(description = "折旧段金额", required = true) private BigDecimal depreciationAmount;
}
