package com.btsheng.erp.business.crm.drawing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * V1.3.9 Sprint 13 Story 13.3 · DrawingLinkListResponse
 *
 * <p>端点 1（GET /drawings/{id}/links?biz_type=ORDER）· 图纸 → 关联业务单据 ID 列表
 * <p>单类 biz_type 过滤 · 无缓存
 * <p>querySource 恒为 DB_REAL（端点 1 不缓存 · 高频但非热点）
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@Data
@Schema(description = "图纸关联业务单据列表响应（13.3 端点 1）")
public class DrawingLinkListResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "图纸 ID", example = "100")
    private Long drawingId;

    @Schema(description = "业务类型",
            example = "ORDER",
            allowableValues = {"ORDER", "PO", "INCOMING", "INSPECTION", "WORKORDER_PROCESS"})
    private String bizType;

    @Schema(description = "关联业务单据 ID 列表", example = "[100, 101]")
    private List<Long> bizIds;

    @Schema(description = "总数（与 bizIds.length 同值 · 前端分页冗余字段）", example = "2")
    private Integer totalCount;

    @Schema(description = "数据来源",
            example = "DB_REAL",
            allowableValues = {"DB_REAL", "CACHE"})
    private String querySource;

    @Schema(description = "查询时间（ISO 8601）")
    private LocalDateTime queriedAt;
}
