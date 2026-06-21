package com.btsheng.erp.production.outsource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.22 · 委外状态机历史（crm_outsource_state_history）
 * FR-6-2 · 7 状态机迁移留痕
 */
@Data
@Schema(description = "委外状态机历史（crm_outsource_state_history · Story 1.22 FR-6-2）")
@TableName("crm_outsource_state_history")
public class CrmOutsourceStateHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("outsource_id")        private Long outsourceId;
    @TableField("outsource_no")        private String outsourceNo;
    @TableField("from_state")          private String fromState;
    @TableField("to_state")            private String toState;
    @TableField("transition_type")     private String transitionType;
    @TableField("operator_user_id")    private Long operatorUserId;
    @TableField("operator_role")       private String operatorRole;
    @TableField("reason")              private String reason;
    @TableField("occurred_at")         private LocalDateTime occurredAt;
}
