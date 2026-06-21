package com.btsheng.erp.production.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "操作工当前工序（GET /workorders/current-process · 5min 缓存）")
public class CurrentProcessResponse {
    private Long userId;
    private Long workorderId;
    private Long drawingId;
    private Long processId;
    private String processNo;
    private String processName;
    private String workorderNo;
    private Integer processSeq;
    private Boolean cached = false;
}
