package com.btsheng.erp.platform.auth.workflow.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审批状态（V1.3.7 · Story 1.2 · P1 修补）
 *
 * <p>状态机：{@code PENDING} → {@code APPROVED} / {@code REJECTED} / {@code SKIPPED}。<br>
 * {@code WAITING} 是 OR 会签下未轮到但已记录在案的占位状态（V1.3.7 P1 修补新增）。
 * 禁止 {@code PENDING → PENDING} 自循环（→ 40904）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Schema(description = "审批状态")
public enum ApprovalStatus {

    PENDING("PENDING", "待审批"),
    WAITING("WAITING", "OR 会签占位（未轮到或全员未决）"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    SKIPPED("SKIPPED", "已跳过（请假/出差/离职/禁用）");

    @JsonValue
    private final String code;
    private final String description;

    ApprovalStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ApprovalStatus fromCode(String code) {
        if (code == null) return null;
        for (ApprovalStatus s : values()) {
            if (s.code.equalsIgnoreCase(code)) {
                return s;
            }
        }
        return null;
    }

    /** 是否为终态 */
    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED;
    }
}
