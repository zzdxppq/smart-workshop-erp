package com.btsheng.erp.business.crm.warehouse.scan.controller;

import com.btsheng.erp.business.crm.warehouse.scan.entity.CrmWarehouseIncomingItem;
import com.btsheng.erp.business.crm.warehouse.scan.service.WarehouseIncomingScanService;
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

@Tag(name = "E12-Warehouse-Incoming-Scan", description = "委外协同·仓管扫码")
@RestController
@RequestMapping("/warehouse/incoming-scan")
public class WarehouseIncomingScanController {

    private final WarehouseIncomingScanService service;

    @Autowired
    public WarehouseIncomingScanController(WarehouseIncomingScanService service) {
        this.service = service;
    }

    @Operation(summary = "创建扫码单（5 类码必传）")
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestParam String permissionNo,
                                                @RequestParam Long userId,
                                                @RequestParam String vendorName,
                                                @RequestParam String outsourceNo,
                                                @RequestParam String email,
                                                @RequestBody List<CrmWarehouseIncomingItem> items) {
        return service.createScan(permissionNo, userId, vendorName, outsourceNo, email, items);
    }

    @Operation(summary = "查询扫码单")
    @GetMapping("/get")
    public Result<Map<String, Object>> get(@RequestParam String scanNo) {
        return service.getScan(scanNo);
    }

    @Operation(summary = "确认扫码")
    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestParam String scanNo) {
        return service.confirmScan(scanNo);
    }

    @Operation(summary = "扫码单列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) Long userId,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "100") int limit) {
        return service.listScans(userId, status, limit);
    }
}
