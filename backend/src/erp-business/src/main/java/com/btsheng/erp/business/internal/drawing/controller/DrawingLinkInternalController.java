package com.btsheng.erp.business.internal.drawing.controller;

import com.btsheng.erp.business.internal.drawing.dto.CreateDrawingLinkRequest;
import com.btsheng.erp.business.internal.drawing.service.DrawingLinkWriteService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/drawing-links")
@Tag(name = "Internal-DrawingLink", description = "图纸关联 · business 库内部接口")
public class DrawingLinkInternalController {

    private final DrawingLinkWriteService writeService;

    public DrawingLinkInternalController(DrawingLinkWriteService writeService) {
        this.writeService = writeService;
    }

    @PostMapping
    @Operation(summary = "创建图纸-业务关联（幂等 · uk_biz_ref）")
    public Result<Void> create(@RequestBody CreateDrawingLinkRequest req) {
        if (req == null) {
            return Result.fail(40001, "DRAWING_LINK_REQUEST_REQUIRED");
        }
        return writeService.createLink(req.getDrawingId(), req.getBizType(), req.getBizId(), req.getCreatedBy());
    }
}
