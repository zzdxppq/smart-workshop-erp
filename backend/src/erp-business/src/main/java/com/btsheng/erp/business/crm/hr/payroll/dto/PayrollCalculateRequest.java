package com.btsheng.erp.business.crm.hr.payroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "薪酬核算请求")
public class PayrollCalculateRequest {
    @Schema(description = "员工ID") private Long employeeId;
    @Schema(description = "年份") private Integer periodYear;
    @Schema(description = "月份") private Integer periodMonth;
    @Schema(description = "加班小时") private BigDecimal overtimeHours = BigDecimal.ZERO;
    @Schema(description = "奖金") private BigDecimal bonus = BigDecimal.ZERO;
    @Schema(description = "扣款") private BigDecimal deduction = BigDecimal.ZERO;
}
