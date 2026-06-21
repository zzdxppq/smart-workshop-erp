package com.btsheng.erp.platform.auth.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流 VO（V1.3.7 · Story 1.2 · T2.1）
 */
@Data
@Schema(description = "工作流视图")
public class WorkflowVO {

    @Schema(description = "工作流 ID")
    private Long id;

    @Schema(description = "工作流名称")
    private String name;

    @Schema(description = "工作流编码")
    private String workflowCode;

    @Schema(description = "节点列表（明文）")
    private List<WorkflowNodeVO> nodes;

    @Schema(description = "条件 JSON（明文）")
    private String conditionsJson;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "版本")
    private Integer version;

    @Schema(description = "最后修改人 user_id")
    private Long lastModifiedBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Data
    @Schema(description = "工作流节点视图")
    public static class WorkflowNodeVO {
        private Integer nodeIndex;
        private String nodeType;
        private String roleCode;
        private java.math.BigDecimal threshold;
        private Boolean orSignRequired;
    }
}
