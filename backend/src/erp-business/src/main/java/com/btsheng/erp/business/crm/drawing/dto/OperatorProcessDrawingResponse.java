package com.btsheng.erp.business.crm.drawing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * V1.3.9 Sprint 13 Story 13.3 · OperatorProcessDrawingResponse
 *
 * <p>端点 3（GET /drawings/process/{processId}）· OPERATOR 工序扫码 → 图纸列表
 * <p>手机端扫码高频场景 · Redis 5min TTL 缓存 · 端到端 < 200ms
 * <p>Key `user:current_process:{user_id}` · @CacheEvict on crm_workorder_process 状态变更
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@Data
@Schema(description = "OPERATOR 工序可访问图纸列表响应（13.3 端点 3）")
public class OperatorProcessDrawingResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "工序 ID", example = "500")
    private Long processId;

    @Schema(description = "工序代号", example = "P-2026-001")
    private String processCode;

    @Schema(description = "工序名称", example = "精车")
    private String processName;

    @Schema(description = "工单 ID", example = "100")
    private Long workOrderId;

    @Schema(description = "工单代号", example = "GD20260612-0001")
    private String workOrderCode;

    @Schema(description = "工序状态",
            example = "IN_PROGRESS",
            allowableValues = {"IN_PROGRESS", "PENDING", "COMPLETED"})
    private String status;

    @Schema(description = "操作工用户 ID", example = "500")
    private Long operatorUserId;

    @Schema(description = "可访问图纸列表")
    private List<ProcessDrawing> drawings;

    @Schema(description = "总数", example = "2")
    private Integer totalCount;

    @Schema(description = "是否 Redis 缓存命中", example = "true")
    private Boolean cacheHit;

    @Schema(description = "查询时间（ISO 8601）")
    private LocalDateTime queriedAt;

    @Data
    @Schema(description = "工序可访问图纸条目")
    public static class ProcessDrawing implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "图纸 ID", example = "100")
        private Long drawingId;

        @Schema(description = "图纸代号", example = "DWG-2026-001")
        private String drawingCode;

        @Schema(description = "图纸名称", example = "齿轮减速机 BWD4")
        private String drawingName;

        @Schema(description = "缩略图 URL", example = "/drawings/100/thumbnail")
        private String thumbnailUrl;

        @Schema(description = "权限级别（OPERATOR 默认 VIEW）",
                example = "VIEW",
                allowableValues = {"VIEW", "PRINT", "EDIT"})
        private String permissionLevel;
    }
}
