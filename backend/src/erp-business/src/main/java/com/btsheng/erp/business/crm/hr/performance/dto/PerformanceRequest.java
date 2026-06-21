package com.btsheng.erp.business.crm.hr.performance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "绩效录入请求")
public class PerformanceRequest {
    @Schema(description = "员工ID") private Long employeeId;
    @Schema(description = "年份") private Integer periodYear;
    @Schema(description = "月份") private Integer periodMonth;
    @Schema(description = "分数 0-100") private BigDecimal score;
    @Schema(description = "KPI 条目") private String kpiItems;
    @Schema(description = "评语") private String comment;
}
