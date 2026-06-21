package com.btsheng.erp.business.crm.drawing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * V1.3.9 Sprint 13 Story 13.3 · AccessibleDrawingListResponse
 *
 * <p>端点 2（GET /drawings/accessible?biz_type=ORDER&biz_id=123）· 业务单据 → 可访问图纸列表
 * <p>12.1 web-impl `<DrawingViewer>` 真实数据接入入口
 * <p>Redis 5min TTL 缓存 · Key `drawing:link:{drawing_id}:{role}:{user_id}`
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@Data
@Schema(description = "业务单据可访问图纸列表响应（13.3 端点 2）")
public class AccessibleDrawingListResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "业务类型", example = "ORDER")
    private String bizType;

    @Schema(description = "业务单据 ID", example = "123")
    private Long bizId;

    @Schema(description = "可访问图纸列表")
    private List<AccessibleDrawing> drawings;

    @Schema(description = "总数", example = "3")
    private Integer totalCount;

    @Schema(description = "是否 Redis 缓存命中", example = "true")
    private Boolean cacheHit;

    @Schema(description = "查询时间（ISO 8601）")
    private LocalDateTime queriedAt;

    @Data
    @Schema(description = "可访问图纸条目")
    public static class AccessibleDrawing implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "图纸 ID", example = "100")
        private Long drawingId;

        @Schema(description = "图纸代号", example = "DWG-2026-001")
        private String drawingCode;

        @Schema(description = "图纸名称", example = "齿轮减速机 BWD4")
        private String drawingName;

        @Schema(description = "版本号", example = "v1.0.0")
        private String version;

        @Schema(description = "缩略图 URL", example = "/drawings/100/thumbnail")
        private String thumbnailUrl;

        @Schema(description = "权限级别",
                example = "VIEW",
                allowableValues = {"VIEW", "PRINT", "EDIT"})
        private String permissionLevel;
    }
}
