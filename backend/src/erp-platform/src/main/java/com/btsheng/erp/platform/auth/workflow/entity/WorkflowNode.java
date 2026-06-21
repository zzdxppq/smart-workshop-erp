package com.btsheng.erp.platform.auth.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btsheng.erp.core.model.BaseDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工作流节点（V1.3.7 · Story 1.2 · P1 修补 ④ 新增物理表）
 *
 * <p>对应 {@code cnc_platform.sys_workflow_node} 表（V2__workflow_split.sql 迁移创建）。
 * 替代 {@code sys_workflow.nodes_json} 中节点的反序列化路径，便于按 {@code node_index ASC} 索引扫描
 * 与 OR 会签判断（{@code or_sign_required} 字段）。
 *
 * <p>V1.3.7 字段重命名：seed 用 {@code role} / {@code node} → 物理表 {@code role_code} / {@code node_index}。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作流节点（sys_workflow_node · P1 修补 ④）")
@TableName("sys_workflow_node")
public class WorkflowNode extends BaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "所属工作流 ID（sys_workflow.id）", example = "1")
    @TableField("workflow_id")
    private Long workflowId;

    @Schema(description = "节点序号 1..N（严格递增）", example = "2")
    @TableField("node_index")
    private Integer nodeIndex;

    @Schema(description = "节点类型（START/APPROVAL/CC/END）", example = "APPROVAL")
    @TableField("node_type")
    private String nodeType;

    @Schema(description = "审批角色编码（APPROVAL 节点必填，引用 sys_role.role_code）", example = "dept_manager")
    @TableField("role_code")
    private String roleCode;

    @Schema(description = "金额阈值（BigDecimal；NULL = 无限额兜底）", example = "200000.00")
    @TableField("threshold")
    private BigDecimal threshold;

    @Schema(description = "OR 会签（V1.3.7 P1 修补：任一候选人通过即推进）", example = "false")
    @TableField("or_sign_required")
    private Boolean orSignRequired;

    @Schema(description = "附加规则 JSON（如 {\"extra_check\": \"credit_limit\"}）")
    @TableField("extra_check_json")
    private String extraCheckJson;
}
