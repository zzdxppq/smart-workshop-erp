package com.btsheng.erp.production.allocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "工序分配看板行")
public class AllocationStepRow {
    private Integer processSeq;
    private String stepName;
    private String equipmentType;
    /** PENDING=待分配 · ALLOCATED=已分配 */
    private String allocationStatus;
    /** INHOUSE / OUTSOURCE，未分配时为 null */
    private String decision;
    private Long allocationId;
    private LocalDateTime decidedAt;
    /** 采购是否已选厂商（已选则不建议生管改归属） */
    private Boolean vendorAssigned;
}
