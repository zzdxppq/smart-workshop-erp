package com.btsheng.erp.production.machine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "设备状态变更")
public class MachineStatusChangeRequest {
    @Schema(description = "目标状态：IDLE / RUNNING / MAINTENANCE / FAULT")
    private String status;
    @Schema(description = "变更原因")
    private String reason;
    @Schema(description = "预计恢复日期（故障/维护时填写）")
    private LocalDate estimatedRecoveryDate;
}