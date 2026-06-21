package com.btsheng.erp.production.scan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "扫码待办响应（AC-5.2.4）")
public class ScanPendingResponse {
    private List<PendingItem> pendingStart;
    private List<PendingItem> pendingReport;
    private List<PendingItem> pendingStation;

    @Data
    public static class PendingItem {
        private String workorderNo;
        private String productName;
        private Integer stepNo;
        private String stepName;
        private String equipmentType;
        private String status;
        private String scheduledStart;
        private String priority;
    }
}
