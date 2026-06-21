package com.btsheng.erp.platform.auth.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工作流试跑结果（V1.3.7 · Story 1.2 · T2.1）
 */
@Data
@Schema(description = "工作流试跑结果")
public class WorkflowTestResult {

    @Schema(description = "匹配节点序号", example = "2")
    private Integer matchedNodeIndex;

    @Schema(description = "匹配角色", example = "dept_manager")
    private String matchedRole;

    @Schema(description = "候选人列表（OR 会签）", example = "[10010, 10011]")
    private List<Long> candidates;

    @Schema(description = "OR 会签（V1.3.7 P1 修补）")
    private Boolean orSignRequired;

    @Schema(description = "路由 trace")
    private List<String> trace;
}
