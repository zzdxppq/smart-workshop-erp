package com.btsheng.erp.business.crm.drawing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * V1.3.7 · Story 1.7 · AC-3.1.3 · 发布审批请求
 *
 * 4 阈值路由：SELF < 5万 / DEPT_MANAGER 5-20万 / GM+财务 > 20万
 * > 20万 → 二次密码（adminPassword 必填）
 * 黑名单优先（CUSTOMER_BLACKLIST 40902 > CREDIT_LIMIT_EXCEEDED 40909）
 */
@Data
@Schema(description = "图纸发布请求")
public class DrawingReleaseRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "管理员二次密码（> 20万 FA 件必填）", example = "")
    private String adminPassword;

    @Schema(description = "审批人列表（OR 会签候选人）")
    private java.util.List<Long> candidates;

    @Schema(description = "审批备注")
    private String comment;
}
