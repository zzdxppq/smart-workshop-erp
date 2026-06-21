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
 * V2.1 · 报价与订单协同设计 · 工单通知记录
 */
@Data
@Schema(description = "工单通知记录（crm_production_notification）")
@TableName("crm_production_notification")
public class CrmProductionNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("planning_id") private Long planningId;               // 排产计划ID
    @TableField("workorder_id") private Long workorderId;             // 关联工单ID
    @TableField("workorder_no") private String workorderNo;         // 工单编号
    @TableField("recipient_type") private String recipientType;   // OPERATOR/ENGINEER/PLANNER/SUPERVISOR
    @TableField("recipient_user_id") private Long recipientUserId;  // 接收人用户ID
    @TableField("recipient_name") private String recipientName;    // 接收人姓名
    @TableField("channel") private String channel;               // APP/PUSH/SMS/EMAIL
    @TableField("title") private String title;                    // 通知标题
    @TableField("content") private String content;               // 通知内容
    @TableField("status") private String status = "PENDING";    // PENDING/SENT/READ/FAILED
    @TableField("sent_at") private LocalDateTime sentAt;         // 发送时间
    @TableField("read_at") private LocalDateTime readAt;         // 阅读时间
    @TableField("created_at") private LocalDateTime createdAt;
}
