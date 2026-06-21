package com.btsheng.erp.platform.auth.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作流更新请求（V1.3.7 · Story 1.2 · T2.1）
 */
@Data
@Schema(description = "工作流更新请求")
public class WorkflowUpdateRequest {

    @Schema(description = "节点 JSON 数组", required = true)
    private String nodesJson;

    @Schema(description = "条件 JSON", required = true)
    private String conditionsJson;

    @Schema(description = "状态：ACTIVE / INACTIVE", example = "ACTIVE")
    private String status;
}
