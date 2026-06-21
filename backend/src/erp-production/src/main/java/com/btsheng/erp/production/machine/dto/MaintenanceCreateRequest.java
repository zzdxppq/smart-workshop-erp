package com.btsheng.erp.production.machine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "添加维保记录")
public class MaintenanceCreateRequest {
    @Schema(description = "维保类型：ROUTINE / PREVENTIVE / REPAIR / OVERHAUL")
    private String maintenanceType;
    @Schema(description = "执行时间")
    private LocalDateTime performedAt;
    @Schema(description = "下次到期时间")
    private LocalDateTime nextDue;
    @Schema(description = "执行人")
    private String executor;
    @Schema(description = "备注")
    private String remark;
}