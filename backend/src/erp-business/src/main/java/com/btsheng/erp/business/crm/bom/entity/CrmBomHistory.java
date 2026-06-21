package com.btsheng.erp.business.crm.bom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.9 · BOM 历史（含转生产工单 GD{yyyyMMdd}{seq:4}）
 */
@Data
@Schema(description = "BOM 历史（crm_bom_history）")
@TableName("crm_bom_history")
public class CrmBomHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("bom_id") private Long bomId;
    @TableField("operation") private String operation;                  // CREATE/UPDATE/RELEASE/ARCHIVE/CONVERT_TO_PRODUCTION
            @TableField("before_json") private String beforeJson;
    @TableField("after_json") private String afterJson;
    @TableField("work_order_no") private String workOrderNo;            // GD{yyyyMMdd}{seq:4}
            @TableField("changed_by") private Long changedBy;
    @TableField("changed_at") private LocalDateTime changedAt;
}
