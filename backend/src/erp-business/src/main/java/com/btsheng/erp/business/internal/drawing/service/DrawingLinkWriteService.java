package com.btsheng.erp.business.internal.drawing.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingLink;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingLinkMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.service.DrawingLinkQueryService;
import com.btsheng.erp.core.model.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * V1.3.8 · 图纸-业务关联写入（跨服务 internal API · erp-production 委外下单等）
 */
@Service
public class DrawingLinkWriteService {

    private final CrmDrawingMapper drawingMapper;
    private final CrmDrawingLinkMapper linkMapper;
    private final DrawingLinkQueryService linkQueryService;

    public DrawingLinkWriteService(CrmDrawingMapper drawingMapper,
                                   CrmDrawingLinkMapper linkMapper,
                                   DrawingLinkQueryService linkQueryService) {
        this.drawingMapper = drawingMapper;
        this.linkMapper = linkMapper;
        this.linkQueryService = linkQueryService;
    }

    @Transactional
    public Result<Void> createLink(Long drawingId, String bizType, Long bizId, Long createdBy) {
        if (drawingId == null) {
            return Result.fail(40001, "DRAWING_ID_REQUIRED");
        }
        if (bizType == null || bizType.isBlank()) {
            return Result.fail(40001, "DRAWING_LINK_BIZ_TYPE_REQUIRED");
        }
        if (bizId == null) {
            return Result.fail(40001, "DRAWING_LINK_BIZ_ID_REQUIRED");
        }
        if (!isSupportedBizType(bizType)) {
            return Result.fail(40001, "DRAWING_LINK_BIZ_TYPE_INVALID");
        }

        CrmDrawing drawing = drawingMapper.selectById(drawingId);
        if (drawing == null) {
            return Result.fail(40404, "DRAWING_NOT_FOUND");
        }

        QueryWrapper<CrmDrawingLink> dup = new QueryWrapper<>();
        dup.eq("biz_type", bizType).eq("biz_id", bizId).eq("drawing_id", drawingId);
        if (linkMapper.selectCount(dup) > 0) {
            return Result.ok(null);
        }

        CrmDrawingLink link = new CrmDrawingLink();
        link.setDrawingId(drawingId);
        link.setBizType(bizType);
        link.setBizId(bizId);
        link.setCreatedBy(createdBy != null ? createdBy : 0L);
        link.setCreatedAt(LocalDateTime.now());
        linkMapper.insert(link);
        linkQueryService.evictAllLinkCaches();
        return Result.ok(null);
    }

    private boolean isSupportedBizType(String bizType) {
        return CrmDrawingLink.BIZ_TYPE_ORDER.equals(bizType)
                || CrmDrawingLink.BIZ_TYPE_PO.equals(bizType)
                || CrmDrawingLink.BIZ_TYPE_INCOMING.equals(bizType)
                || CrmDrawingLink.BIZ_TYPE_INSPECTION.equals(bizType)
                || CrmDrawingLink.BIZ_TYPE_WORKORDER_PROCESS.equals(bizType)
                || CrmDrawingLink.BIZ_TYPE_OUTSOURCE.equals(bizType);
    }
}
