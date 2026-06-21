package com.btsheng.erp.business.crm.bom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.9 · AC-3.3.2 修改 BOM 请求（仅 DRAFT 状态可改）
 */
@Data
@Schema(description = "BOM 修改请求（PUT /boms/{id} · 仅 DRAFT）")
public class BomUpdateRequest {

    @Schema(description = "目标数量（正整数）", example = "200")
    private Integer targetQty;

    @Schema(description = "备注")
    private String comment;

    @Schema(description = "是否允许物料替代")
    private Boolean isSubstitutable;
}
