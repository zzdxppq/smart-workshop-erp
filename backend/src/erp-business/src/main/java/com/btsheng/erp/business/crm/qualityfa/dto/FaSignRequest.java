package com.btsheng.erp.business.crm.qualityfa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * FA 双签确认请求 DTO
 * V2.1 品质专项增强
 */
@Data
@Schema(description = "FA 双签确认请求")
public class FaSignRequest {

    @NotNull(message = "FA_ID_REQUIRED")
    @Schema(description = "FA 主键ID")
    private Long faId;

    @Schema(description = "签字意见（可选）")
    private String comment;

    @Schema(description = "是否通过（默认 true）")
    private Boolean passed = true;
}
