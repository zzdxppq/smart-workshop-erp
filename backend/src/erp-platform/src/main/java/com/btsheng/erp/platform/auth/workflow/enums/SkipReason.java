package com.btsheng.erp.platform.auth.workflow.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 跳过原因（V1.3.7 · Story 1.2 · P1 修补）
 *
 * <p>{@code SkipOnLeaveRule} 读取 platform 本地 {@code sys_user.availability_status}（由 business HR 同步）后命中：
 * <ul>
 *   <li>{@code ON_LEAVE} - 请假（核心场景）</li>
 *   <li>{@code ON_TRIP} - 出差</li>
 *   <li>{@code DISABLED} - 账号被禁用</li>
 *   <li>{@code RESIGNED} - 离职</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Schema(description = "跳过原因（V1.3.7 P1 修补）")
public enum SkipReason {

    ON_LEAVE("ON_LEAVE", "请假"),
    ON_TRIP("ON_TRIP", "出差"),
    DISABLED("DISABLED", "账号禁用"),
    RESIGNED("RESIGNED", "离职");

    @JsonValue
    private final String code;
    private final String description;

    SkipReason(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SkipReason fromCode(String code) {
        if (code == null) return null;
        for (SkipReason r : values()) {
            if (r.code.equalsIgnoreCase(code)) {
                return r;
            }
        }
        return null;
    }
}
