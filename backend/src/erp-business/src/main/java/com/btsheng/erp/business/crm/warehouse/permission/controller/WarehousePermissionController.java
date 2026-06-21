package com.btsheng.erp.business.crm.warehouse.permission.controller;

import com.btsheng.erp.business.crm.warehouse.permission.service.WarehousePermissionService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "E12-Warehouse-Permission", description = "委外协同·仓管扫码权限")
@RestController
@RequestMapping("/warehouse/permission")
public class WarehousePermissionController {

    private final WarehousePermissionService service;

    @Autowired
    public WarehousePermissionController(WarehousePermissionService service) {
        this.service = service;
    }

    @Operation(summary = "授予仓管扫码权限（8h 有效）")
    @PostMapping("/grant")
    public Result<Map<String, Object>> grant(@RequestParam Long userId,
                                              @RequestParam String userName,
                                              @RequestParam String role,
                                              @RequestParam String grantedBy,
                                              @RequestParam(required = false) String email) {
        return service.grantPermission(userId, userName, role, grantedBy, email);
    }

    @Operation(summary = "查询扫码权限")
    @GetMapping("/get")
    public Result<Map<String, Object>> get(@RequestParam String permissionNo) {
        return service.getPermission(permissionNo);
    }
}
