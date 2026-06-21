package com.btsheng.erp.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 报价审批路由结果（V1.3.7 · P1 修补）
 *
 * <p>{@code candidates} 字段是 architect 评审 P1 修补项：
 * 即使 V1.3.7 不消费（仍取 {@code user_id ASC} 第一人），也必须输出以降低 Story 1.2 升级成本。
 */
@Schema(description = "报价审批路由结果")
public class QuoteApprovalResult {

    @Schema(description = "审批人 user_id（V1.3.7 取 candidates[0]）", example = "10010")
    private Long approverUserId;

    @Schema(description = "当前审批节点（salesperson / dept_manager / gm）", example = "dept_manager")
    private String currentNode;

    @Schema(description = "候选审批人列表（V1.3.7 P1 修补：Story 1.2 工作流引擎升级 OR 会签用）", example = "[10010, 10011]")
    private List<Long> candidates;

    @Schema(description = "路由决策说明", example = "金额 60000 > salesperson 阈值 50000，路由到 dept_manager")
    private String reason;

    public Long getApproverUserId() { return approverUserId; }
    public void setApproverUserId(Long approverUserId) { this.approverUserId = approverUserId; }
    public String getCurrentNode() { return currentNode; }
    public void setCurrentNode(String currentNode) { this.currentNode = currentNode; }
    public List<Long> getCandidates() { return candidates; }
    public void setCandidates(List<Long> candidates) { this.candidates = candidates; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
