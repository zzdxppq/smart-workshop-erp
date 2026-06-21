package com.btsheng.erp.business.crm.warehouselocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "创建库位请求（AC-4.3.1 · 库位编码 LOC-A01-01-01）")
public class LocationCreateRequest {
    @Schema(description = "库位编码", required = true)
    private String locationCode;
    @Schema(description = "仓库编码 WH-A", required = true)
    private String warehouse;
    @Schema(description = "库区 A01", required = true)
    private String zone;
    @Schema(description = "库位 01", required = true)
    private String position;
    @Schema(description = "库容")
    private BigDecimal capacity;
}
