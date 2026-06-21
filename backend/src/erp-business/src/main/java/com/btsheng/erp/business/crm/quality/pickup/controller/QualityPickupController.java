package com.btsheng.erp.business.crm.quality.pickup.controller;

import com.btsheng.erp.business.crm.quality.pickup.entity.CrmQualityPickupItem;
import com.btsheng.erp.business.crm.quality.pickup.service.QualityPickupService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "E12-Quality-Pickup", description = "委外协同·品质领料后质检")
@RestController
@RequestMapping("/quality/pickup")
public class QualityPickupController {

    private final QualityPickupService service;

    @Autowired
    public QualityPickupController(QualityPickupService service) {
        this.service = service;
    }

    @Operation(summary = "创建品质领料单")
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestParam String scanNo,
                                                @RequestParam Long inspectorId,
                                                @RequestParam String inspectorName,
                                                @RequestParam String vendorName,
                                                @RequestParam String email,
                                                @RequestBody List<CrmQualityPickupItem> items) {
        return service.createPickup(scanNo, inspectorId, inspectorName, vendorName, email, items);
    }

    @Operation(summary = "查询领料单")
    @GetMapping("/get")
    public Result<Map<String, Object>> get(@RequestParam String pickupNo) {
        return service.getPickup(pickupNo);
    }

    @Operation(summary = "执行质检")
    @PostMapping("/inspect")
    public Result<Map<String, Object>> inspect(@RequestParam String pickupNo,
                                                  @RequestBody List<CrmQualityPickupItem> inspectResults) {
        return service.inspectPickup(pickupNo, inspectResults);
    }
}
