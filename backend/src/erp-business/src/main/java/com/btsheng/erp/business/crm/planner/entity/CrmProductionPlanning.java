package com.btsheng.erp.business.crm.planner.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V2.1 · 报价与订单协同设计 · 排产计划
 */
@Data
@Schema(description = "排产计划（crm_production_planning）")
@TableName("crm_production_planning")
public class CrmProductionPlanning implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_id") private Long orderId;                 // 销售订单ID
    @TableField("workbench_id") private Long workbenchId;         // 工程转化工作台ID
    @TableField("planning_no") private String planningNo;       // 排产计划编号
    @TableField("status") private String status = "PENDING";    // PENDING/ASSIGNED/SCHEDULED/IN_PRODUCTION/COMPLETED
    @TableField("planned_start") private LocalDateTime plannedStart;  // 计划开始
    @TableField("planned_end") private LocalDateTime plannedEnd;      // 计划结束
    @TableField("actual_start") private LocalDateTime actualStart;    // 实际开始
    @TableField("actual_end") private LocalDateTime actualEnd;        // 实际结束
    @TableField("planner_user_id") private Long plannerUserId;      // 生管用户ID
    @TableField("planner_name") private String plannerName;          // 生管姓名
    @TableField("remark") private String remark;                      // 备注
    @TableField("cancel_reason") private String cancelReason;       // 取消原因
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
