package com.btsheng.erp.production.outsource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "委外查询")
public class OutsourceQueryRequest {
    @Schema(description = "状态 DRAFT/SENT/ACCEPTED/IN_PRODUCTION/INSPECTED/COMPLETED/CLOSED/REWORK")
    private String status;
    @Schema(description = "工单号")
    private String workorderNo;
    @Schema(description = "供应商 ID")
    private Long supplierId;
    @Schema(description = "页码")
    private Integer page = 0;
    @Schema(description = "页大小")
    private Integer size = 20;
}
