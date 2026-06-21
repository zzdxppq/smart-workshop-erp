package com.btsheng.erp.business.crm.warehouselocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新仓库请求")
public class WarehouseUpdateRequest {
    private String warehouseName;
    private String warehouseType;
    private String address;
    private Long managerUserId;
    private Integer isActive;
}
