package com.btsheng.erp.business.crm.procurementapproval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.8 · Story 4.2 · 审批路由预览请求 DTO（无副作用）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "审批路由预览请求（Story 4.2 AC-4.2.2 · 无副作用）")
public class ApprovalRouteRequest {

    @Schema(description = "金额", example = "30000", required = true)
    @NotNull
    private BigDecimal amount;

    @Schema(description = "物料品类", example = "TOOL")
    private String category;

    @Schema(description = "供应商状态", example = "NORMAL")
    private String supplierStatus;

    @Schema(description = "紧急度 NORMAL/URGENT", example = "NORMAL")
    private String urgency;
}