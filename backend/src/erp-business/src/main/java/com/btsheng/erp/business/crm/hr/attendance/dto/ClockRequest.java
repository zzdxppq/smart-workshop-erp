package com.btsheng.erp.business.crm.hr.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "考勤打卡请求")
public class ClockRequest {
    @Schema(description = "员工ID") private Long employeeId;
    @Schema(description = "打卡类型 IN/OUT/LUNCH_IN/LUNCH_OUT") private String clockType;
    @Schema(description = "打卡时间（可选；缺省=now）") private LocalDateTime clockAt;
    @Schema(description = "备注") private String remark;
}
