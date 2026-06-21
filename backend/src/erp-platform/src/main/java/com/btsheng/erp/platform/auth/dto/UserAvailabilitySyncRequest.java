package com.btsheng.erp.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "同步用户可用性（erp-business HR → erp-platform）")
public class UserAvailabilitySyncRequest {

    @Schema(description = "ON_DUTY/ON_LEAVE/ON_TRIP/RESIGNED", example = "ON_LEAVE")
    private String availabilityStatus;

    @Schema(description = "请假单号", example = "HR-2026-06-10-001")
    private String leaveNo;
}
