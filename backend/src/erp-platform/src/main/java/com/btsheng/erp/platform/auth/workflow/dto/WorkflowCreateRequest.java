package com.btsheng.erp.platform.auth.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作流创建请求（V1.3.7 · Story 1.2 · T2.1）
 */
@Data
@Schema(description = "工作流创建请求")
public class WorkflowCreateRequest {

    @Schema(description = "工作流编码（^[A-Z_]{2,50}$）", example = "CUSTOM_FLOW")
    private String workflowCode;

    @Schema(description = "节点 JSON 数组（1..20 节点；含 START + APPROVAL* + END）")
    private String nodesJson;

    @Schema(description = "条件 JSON（amount_field 必填）")
    private String conditionsJson;
}
