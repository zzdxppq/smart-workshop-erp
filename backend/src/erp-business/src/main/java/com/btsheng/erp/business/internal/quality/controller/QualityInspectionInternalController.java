package com.btsheng.erp.business.internal.quality.controller;

import com.btsheng.erp.business.crm.qualityinspection.dto.PendingInspectionRequest;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspection;
import com.btsheng.erp.business.crm.qualityinspection.service.QualityInspectionAutoPushService;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/quality-inspections")
public class QualityInspectionInternalController {

    private final QualityInspectionAutoPushService autoPushService;

    @Autowired
    public QualityInspectionInternalController(QualityInspectionAutoPushService autoPushService) {
        this.autoPushService = autoPushService;
    }

    @PostMapping("/pending")
    public Result<Map<String, Object>> createPending(@RequestBody PendingInspectionRequest req,
                                                     @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        Result<CrmQualityInspection> created = autoPushService.createPending(req, userId);
        if (created.getCode() != 0) {
            return Result.fail(created.getCode(), created.getMessage());
        }
        CrmQualityInspection q = created.getData();
        Map<String, Object> data = new HashMap<>();
        data.put("inspectionId", q.getId());
        data.put("inspectionNo", q.getInspectionNo());
        data.put("status", q.getResult());
        return Result.ok(data);
    }
}
