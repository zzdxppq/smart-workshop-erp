package com.btsheng.erp.production.mrp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "MRP 运行请求（AC-5.3.1）")
public class MrpRunRequest {
    @Schema(description = "起始日期", required = true)
    private LocalDate dateRangeStart;
    @Schema(description = "结束日期", required = true)
    private LocalDate dateRangeEnd;
    @Schema(description = "仓库 ID 列表", required = true)
    private List<Long> warehouseIds;
    @Schema(description = "运算类型 FULL/INCREMENTAL")
    private String runType = "FULL";
    @Schema(description = "触发方式 MANUAL/EVENT/SCHEDULED")
    private String triggerType = "MANUAL";
    @Schema(description = "触发来源说明")
    private String triggerSource;
}
