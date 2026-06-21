package com.btsheng.erp.business.crm.procurementapproval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * V1.3.8 · Story 4.2 · 审批路由预览响应 DTO
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "审批路由预览响应（Story 4.2 AC-4.2.2）")
public class ApprovalRouteResponse {

    @Schema(description = "审批路由角色列表（V1.3.8 PROCUREMENT_MANAGER 加入）")
    private List<String> route;

    @Schema(description = "命中的阈值标识（AMOUNT_10K_50K / CATEGORY_TOOL / ...）")
    private List<String> matchedThresholds;

    @Schema(description = "预估审批人数（GM+PM 双签 = 2）")
    private Integer estimatedSigners;

    @Schema(description = "兼容老路由（V1.3.7 DEPT_MANAGER 兜底）")
    private List<String> compatibleLegacyRoute;
}