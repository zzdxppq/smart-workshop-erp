package com.btsheng.erp.platform.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("email_config")
public class EmailConfig implements Serializable {
    @TableId
    private Integer id;
    @TableField("smtp_host") private String smtpHost;
    @TableField("smtp_port") private Integer smtpPort;
    @TableField("use_ssl") private Boolean useSsl;
    @TableField("from_address") private String fromAddress;
    @TableField("auth_code_kek") private String authCodeKek;
    @TableField("retry_policy") private String retryPolicy;
    @TableField("daily_quota") private Integer dailyQuota;
    @TableField("warn_threshold") private BigDecimal warnThreshold;
    @TableField("log_retention_days") private Integer logRetentionDays;
    @TableField("attachment_max_size_mb") private Integer attachmentMaxSizeMb;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
