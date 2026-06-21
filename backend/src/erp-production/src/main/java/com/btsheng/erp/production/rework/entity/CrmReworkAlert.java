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
 * V1.3.7 · Story 1.23 · 返修次数预警
 */
@Data
@Schema(description = "返修次数预警（crm_rework_alert · Story 1.23）")
@TableName("crm_rework_alert")
public class CrmReworkAlert implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("outsource_id")   private Long outsourceId;
    @TableField("outsource_no")   private String outsourceNo;
    @TableField("rework_count")   private Integer reworkCount;
    @TableField("alert_level")    private String alertLevel;
    @TableField("alert_message")  private String alertMessage;
    @TableField("alerted_at")     private LocalDateTime alertedAt;
}
