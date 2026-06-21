package com.btsheng.erp.business.internal.drawing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "内部 · 创建图纸-业务关联")
public class CreateDrawingLinkRequest {

    private Long drawingId;
    private String bizType;
    private Long bizId;
    private Long createdBy;
}
