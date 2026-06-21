package com.btsheng.erp.business.crm.rfq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.32 · 中标请求
 */
@Data
@Schema(description = "中标请求（Story 1.32 FR-8-1）")
public class AwardRfqRequest {

    @Schema(description = "厂商 ID（指定中标）", example = "901")
    private Long vendorId;

    @Schema(description = "是否自动触发 PO 闭环", example = "true")
    private Boolean autoCreatePo = true;
}
