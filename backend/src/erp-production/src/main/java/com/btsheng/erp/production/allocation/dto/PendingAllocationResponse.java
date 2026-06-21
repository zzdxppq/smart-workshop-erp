package com.btsheng.erp.production.allocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "待委外工序清单项")
public class PendingAllocationResponse {
    private Long id;
    private Long workorderId;
    private Integer processSeq;
    private String decision;
    private Long decidedByUserId;
    private LocalDateTime decidedAt;
    private String workorderNo;
    private String productCode;
    private String processName;
}
