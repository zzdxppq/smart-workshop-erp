package com.btsheng.erp.business.crm.warehouselocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "更新库位请求")
public class LocationUpdateRequest {
    private String zone;
    private String position;
    private BigDecimal capacity;
    private Integer isActive;
}
