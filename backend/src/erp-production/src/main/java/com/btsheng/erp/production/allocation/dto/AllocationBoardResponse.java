package com.btsheng.erp.production.allocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "工序分配看板（生管 · E5-S4 AC-5.4.1）")
public class AllocationBoardResponse {
    private Long workorderId;
    private String workorderNo;
    private String materialCode;
    private String productName;
    private int totalSteps;
    private int pendingCount;
    private int allocatedCount;
    private int outsourcePendingForBuyer;
    private List<AllocationStepRow> pendingSteps = new ArrayList<>();
    private List<AllocationStepRow> allocatedSteps = new ArrayList<>();
}
