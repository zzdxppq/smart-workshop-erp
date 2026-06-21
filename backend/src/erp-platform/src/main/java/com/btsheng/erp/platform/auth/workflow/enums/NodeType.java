package com.btsheng.erp.platform.auth.workflow.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流节点类型（V1.3.7 · Story 1.2 · BR-4）
 *
 * <p>{@code START}（node_index=1）→ {@code APPROVAL} → ... → {@code END}（node_index=N）。
 * 中间可插入 {@code CC}（抄送）节点。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Schema(description = "工作流节点类型")
public enum NodeType {

    START("START", "开始节点"),
    APPROVAL("APPROVAL", "审批节点（指定 role_code + threshold）"),
    CC("CC", "抄送节点（不阻塞流程）"),
    END("END", "结束节点");

    @JsonValue
    private final String code;
    private final String description;

    NodeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static NodeType fromCode(String code) {
        if (code == null) return null;
        for (NodeType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        return null;
    }
}
