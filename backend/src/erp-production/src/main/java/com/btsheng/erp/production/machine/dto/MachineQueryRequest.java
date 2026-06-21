package com.btsheng.erp.production.machine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "设备列表查询")
public class MachineQueryRequest {
    private String keyword;
    private String machineType;
    private String status;
    private int page = 0;
    private int size = 20;
}
