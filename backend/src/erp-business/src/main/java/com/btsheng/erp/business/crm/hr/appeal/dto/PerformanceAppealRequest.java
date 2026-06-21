package com.btsheng.erp.business.crm.hr.appeal.dto;

import lombok.Data;

@Data
public class PerformanceAppealRequest {
    private Long performanceId;
    private String reason;
}
