package com.btsheng.erp.business.crm.qualityinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "让步接收审批请求")
public class ConcessionApproveRequest {

    @Schema(description = "QUALITY_MANAGER / PRODUCTION_MANAGER", required = true)
    private String approverRole;

    @Schema(description = "APPROVE / REJECT", required = true)
    private String action;

    private String comment;
}
