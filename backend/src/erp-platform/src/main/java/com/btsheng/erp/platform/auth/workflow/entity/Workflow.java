package com.btsheng.erp.platform.auth.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btsheng.erp.core.model.PlatformAuditDO;
import com.btsheng.erp.core.web.AesGcmTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 工作流配置（V1.3.7 · Story 1.2 · T0.1）
 *
 * <p>对应 {@code cnc_platform.sys_workflow} 表（init.sql:90-102）。
 * V1.3.7 P1 修补 ④：节点拆分到 {@link WorkflowNode} 物理表做细粒度查询，
 * {@code nodes_json} 保留为冗余快照便于 V1.3.4 Stream 重放。
 *
 * <p>字段级加密：{@code nodesJson} AES-256-GCM（V1.3.6/7 红线 · 复用 AesGcmTypeHandler）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作流配置（sys_workflow）")
@TableName(value = "sys_workflow", autoResultMap = true)
public class Workflow extends PlatformAuditDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "工作流编码（UNIQUE，全大写 + 下划线）", example = "QUOTE_FLOW")
    @TableField("workflow_code")
    private String workflowCode;

    @Schema(description = "节点 JSON（AES-256-GCM 加密存储）")
    @TableField(value = "nodes_json", typeHandler = AesGcmTypeHandler.class)
    private String nodesJson;

    @Schema(description = "条件 JSON（amount_field 等）")
    @TableField("conditions_json")
    private String conditionsJson;

    @Schema(description = "状态：ACTIVE / INACTIVE / DELETED", example = "ACTIVE")
    @TableField("status")
    private String status;

    @Schema(description = "V1.3.7 留 hook，给后续灰度发布用", example = "1")
    @TableField("version")
    private Integer workflowVersion;

    @Schema(description = "最后修改人 user_id")
    @TableField("last_modified_by")
    private Long lastModifiedBy;
}
