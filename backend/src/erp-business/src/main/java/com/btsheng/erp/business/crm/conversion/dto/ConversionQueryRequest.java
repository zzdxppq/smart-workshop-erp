package com.btsheng.erp.business.crm.conversion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.8 · AC-3.2.2 转化列表查询
 */
@Data
@Schema(description = "工程转化列表查询")
public class ConversionQueryRequest {
    @Schema(description = "页码（0 起）", example = "0")
    private Integer page = 0;
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;
    @Schema(description = "状态过滤：CONVERTED/FAILED", example = "CONVERTED")
    private String status;
}
