package com.btsheng.erp.production.machine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新设备机台")
public class MachineUpdateRequest {
    private String machineName;
    private String machineType;
    private String machineNo;
    private String status;
    private Integer maintenanceCycleDays;
    private String remark;
    private Boolean active;
}
