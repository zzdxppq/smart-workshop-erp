package com.btsheng.erp.business.crm.warehouselocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建仓库请求（AC-4.3.1）")
public class WarehouseCreateRequest {
    @Schema(description = "仓库编码 WH-A", required = true)
    private String warehouseCode;
    @Schema(description = "仓库名称", required = true)
    private String warehouseName;
    @Schema(description = "仓库类型 MAIN/SUB/LINE_SIDE", required = true)
    private String warehouseType;
    @Schema(description = "地址")
    private String address;
    @Schema(description = "管理员 user_id")
    private Long managerUserId;
}
