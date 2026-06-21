package com.btsheng.erp.business.crm.warehouselocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建盘点单请求")
public class StocktakeCreateRequest {
    @Schema(description = "仓库编码", example = "WH-01")
    private String warehouseCode;
}
