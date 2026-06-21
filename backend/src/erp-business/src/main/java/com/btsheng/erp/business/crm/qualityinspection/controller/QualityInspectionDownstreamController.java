package com.btsheng.erp.business.crm.qualityinspection.controller;

import com.btsheng.erp.business.crm.qualityinspection.dto.QualityDownstreamCreateRequest;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspection;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityInspectionMapper;
import com.btsheng.erp.business.crm.qualityinspection.service.QualityInspectionDispositionService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/quality")
@Tag(name = "E7-Quality-Downstream", description = "品质下游单据")
public class QualityInspectionDownstreamController {

    private final CrmQualityInspectionMapper inspectionMapper;
    private final QualityInspectionDispositionService dispositionService;

    @Autowired
    public QualityInspectionDownstreamController(CrmQualityInspectionMapper inspectionMapper,
                                                 QualityInspectionDispositionService dispositionService) {
        this.inspectionMapper = inspectionMapper;
        this.dispositionService = dispositionService;
    }

    @PostMapping("/return-orders")
    @Operation(summary = "创建退货单（一般由检验提交自动触发）")
    public Result<Map<String, Object>> createReturn(@RequestBody QualityDownstreamCreateRequest req,
                                                    @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return create(req, QualityInspectionDispositionService.TYPE_RETURN, userId);
    }

    @PostMapping("/rework-orders")
    @Operation(summary = "创建返工工单")
    public Result<Map<String, Object>> createRework(@RequestBody QualityDownstreamCreateRequest req,
                                                     @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return create(req, QualityInspectionDispositionService.TYPE_REWORK, userId);
    }

    @PostMapping("/scrap-records")
    @Operation(summary = "创建报废记录")
    public Result<Map<String, Object>> createScrap(@RequestBody QualityDownstreamCreateRequest req,
                                                    @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return create(req, QualityInspectionDispositionService.TYPE_SCRAP, userId);
    }

    private Result<Map<String, Object>> create(QualityDownstreamCreateRequest req, String type, Long userId) {
        if (req == null || req.getInspectionId() == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        CrmQualityInspection insp = inspectionMapper.selectById(req.getInspectionId());
        if (insp == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        int qty = req.getQty() == null || req.getQty() <= 0 ? 1 : req.getQty();
        return switch (type) {
            case QualityInspectionDispositionService.TYPE_RETURN -> dispositionService.createReturnOrder(insp, qty, userId);
            case QualityInspectionDispositionService.TYPE_REWORK -> dispositionService.createReworkOrder(insp, qty, userId);
            case QualityInspectionDispositionService.TYPE_SCRAP -> dispositionService.createScrapRecord(insp, qty, userId);
            default -> Result.fail(40001, "TYPE_INVALID");
        };
    }
}
