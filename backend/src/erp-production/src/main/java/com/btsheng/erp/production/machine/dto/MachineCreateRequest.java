package com.btsheng.erp.production.machine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建设备机台")
public class MachineCreateRequest {
    private String machineName;
    private String machineType;
    private String machineNo;
    private String status = "IDLE";
    private Integer maintenanceCycleDays = 90;
    private String remark;
}
