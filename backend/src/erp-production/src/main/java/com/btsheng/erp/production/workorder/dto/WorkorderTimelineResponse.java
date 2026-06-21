package com.btsheng.erp.production.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "工单 timeline 响应（AC-5.1.3 · 全流程节点）")
public class WorkorderTimelineResponse {
    private String workorderNo;
    private String status;
    private List<TimelineNode> nodes;

    @Data
    public static class TimelineNode {
        private String nodeName;       // CREATE / SCHEDULE / START / REPORT / FINISH
            private String operatedAt;
        private Long operatorUserId;
        private String operatorName;
        private String detail;
    }
}
