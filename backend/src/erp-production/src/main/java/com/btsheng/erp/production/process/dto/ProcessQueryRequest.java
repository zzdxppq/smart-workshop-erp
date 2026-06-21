package com.btsheng.erp.production.process.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.10 · 工艺查询
 */
@Data
@Schema(description = "工艺列表查询")
public class ProcessQueryRequest {
    @Schema(description = "页码", example = "0")
    private Integer page = 0;
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;
    @Schema(description = "工艺类型过滤")
    private String processType;
    @Schema(description = "图纸 ID 过滤")
    private Long drawingId;
}
