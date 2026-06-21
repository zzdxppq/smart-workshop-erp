package com.btsheng.erp.business.crm.bom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.9 · BOM 列表查询
 */
@Data
@Schema(description = "BOM 列表查询请求")
public class BomQueryRequest {
    @Schema(description = "页码", example = "0")
    private Integer page = 0;
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;
    @Schema(description = "状态过滤：DRAFT/RELEASED/ARCHIVED", example = "RELEASED")
    private String status;
    @Schema(description = "BOM 类型过滤", example = "STANDARD")
    private String bomType;
    @Schema(description = "图号过滤")
    private String drawingNo;
}
