package com.btsheng.erp.business.crm.bom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.9 · AC-3.3.5 BOM 发布请求
 */
@Data
@Schema(description = "BOM 发布请求（POST /boms/{id}/publish）")
public class ReleaseBomRequest {
    @Schema(description = "管理员密码（FA 件 > 20万 必填 · 复用 1.5 二次密码）")
    private String adminPassword;

    @Schema(description = "备注")
    private String comment;
}
