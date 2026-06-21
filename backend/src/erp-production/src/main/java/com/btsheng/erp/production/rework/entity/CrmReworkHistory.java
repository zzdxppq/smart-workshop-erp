package com.btsheng.erp.production.rework.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.23 · 返修历史
 */
@Data
@Schema(description = "返修历史（crm_rework_history · Story 1.23）")
@TableName("crm_rework_history")
public class CrmReworkHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("rework_id")     private Long reworkId;
    @TableField("operation")     private String operation;
    @TableField("before_json")   private String beforeJson;
    @TableField("after_json")    private String afterJson;
    @TableField("changed_by")    private Long changedBy;
    @TableField("changed_at")    private LocalDateTime changedAt;
}
