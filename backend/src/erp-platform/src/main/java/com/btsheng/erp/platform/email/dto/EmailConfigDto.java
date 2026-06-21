package com.btsheng.erp.platform.email.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EmailConfigDto {
    private String smtpHost;
    private Integer smtpPort;
    private Boolean useSsl;
    private String fromAddress;
    /** 写入专用，读取时不返回 */
    private String authCode;
    private List<String> retryPolicy;
    private Integer dailyQuota;
    private BigDecimal quotaWarnThreshold;
    private Integer logRetentionDays;
    private Integer attachmentMaxSizeMb;
}
