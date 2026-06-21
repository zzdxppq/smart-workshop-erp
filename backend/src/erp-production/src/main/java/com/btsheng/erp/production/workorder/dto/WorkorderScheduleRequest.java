package com.btsheng.erp.production.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "工单排产请求（AC-5.1.2 · 算法：机台负载 + 工艺先后 + 工时）")
public class WorkorderScheduleRequest {
    @Schema(description = "机台 ID", required = true)
    private Long equipmentId;
    @Schema(description = "机台类型")
    private String equipmentType;
    @Schema(description = "计划开始时间", required = true)
    private LocalDateTime planStart;
    @Schema(description = "计划结束时间", required = true)
    private LocalDateTime planEnd;
    @Schema(description = "是否允许覆盖冲突")
    private Boolean forceOverride = false;
}
